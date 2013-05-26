package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ComponentPopupBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.AbstractPopup
import com.johnlindquist.acejump.keycommands.*
import com.johnlindquist.acejump.ui.AceCanvas
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.impl.IdeFocusManagerImpl
import com.intellij.openapi.wm.IdeFocusManager

public open class AceJumpAction(val afterCharMode: Boolean = false): DumbAwareAction() {

    override public fun update(e: AnActionEvent?) {
        e?.getPresentation()?.setEnabled((e?.getData(PlatformDataKeys.EDITOR)) != null)
    }

    override public fun actionPerformed(anActionEvent: AnActionEvent?) {
        val actionEvent = anActionEvent
        val project = actionEvent?.getData(PlatformDataKeys.PROJECT) as Project
        val editor = actionEvent?.getData(PlatformDataKeys.EDITOR) as EditorImpl
        val virtualFile = actionEvent?.getData(PlatformDataKeys.VIRTUAL_FILE) as VirtualFile
        val document = editor.getDocument() as DocumentImpl
        val scheme = EditorColorsManager.getInstance()?.getGlobalScheme()
        val font = Font(scheme?.getEditorFontName(), Font.BOLD, scheme?.getEditorFontSize()!!)
        val aceFinder = AceFinder(project, document, editor, virtualFile)
        val aceJumper = AceJumper(editor, document)
        val aceCanvas = AceCanvas()
        val searchBox = SearchBox()
        val textAndOffsetHash = HashMap<String, Int>()

        aceFinder.isAfterCharMode = afterCharMode

        fun showJumpers(textPointPairs: List<Pair<String, Point>>?) {
            aceCanvas.jumpInfos = textPointPairs?.reverse()
            aceCanvas.repaint()
        }
        fun exit() {
            val contentComponent: JComponent? = editor.getContentComponent()
            contentComponent?.remove(aceCanvas)
            contentComponent?.repaint()
            textAndOffsetHash.clear()
        }

        //todo: refactor
        /*
            If there are 26 or less points, use A-Z
            If there are >26, then start A-Y then ZA-ZZ
            A huge list would be like A-C then DA-ZZ
        */
        fun setupJumpLocations(results: MutableList<Int>) {

            if(results.size == 0) return //todo: hack, in case random keystrokes make it through
            textAndOffsetHash.clear()
            val textPointPairs: MutableList<Pair<String, Point>> = ArrayList<Pair<String, Point>>()
            val total = results.size - 1

            val letters = aceFinder.getAllowedCharacters()!!
            var len = letters.length
            var groups = Math.floor(total.toDouble() / len)
            //            print("groups: " + groups.toString())
            val lenMinusGroups = len - groups.toInt()
            //            print("last letter: " + letters.charAt(lenMinusGroups).toString() + "\n")

            for (i in 0..total) {

                var str = ""

                val iGroup = i - lenMinusGroups
                val iModGroup = iGroup % len
                //                if(iModGroup == 0) print("================\n")
                val i1 = Math.floor(lenMinusGroups.toDouble() + ((i + groups.toInt()) / len)).toInt() - 1
                if(i >= lenMinusGroups){
                    str += letters.charAt(i1)
                    str += letters.charAt(iModGroup).toString()
                }else {
                    str += letters.charAt(i).toString()
                }
                //                print(i.toString() + ": " + str + "     iModGroup:" + iModGroup.toString() + "\n")


                val textOffset: Int = results.get(i)
                val point: RelativePoint? = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset))
                textPointPairs.add(Pair<String, Point>(str, point?.getOriginalPoint() as Point))
                textAndOffsetHash.put(str, textOffset)

                if(str == "zz"){
                    break
                }
            }
            showJumpers(textPointPairs)
        }


        fun addAceCanvas() {
            val contentComponent: JComponent? = editor.getContentComponent()
            contentComponent?.add(aceCanvas)
            val viewport = editor.getScrollPane().getViewport()
            aceCanvas.setBounds(0, 0, (viewport?.getWidth())!! + 1000, (viewport?.getHeight())!! + 1000)
            val rootPane: JRootPane? = editor.getComponent().getRootPane()!!
            val locationOnScreen: Point? = SwingUtilities.convertPoint(aceCanvas, (aceCanvas.getLocation()), rootPane)
            aceCanvas.setLocation(-locationOnScreen!!.x, -locationOnScreen.y)
        }

        fun configureSearchBox() {
            fun setupSearchBoxKeys() {
                val showJumpObserver: ChangeListener = object : ChangeListener {
                    public override fun stateChanged(e: ChangeEvent) {
                        setupJumpLocations(aceFinder.results as MutableList<Int>)
                    }
                }
                val releasedHome: AceKeyCommand = ShowBeginningOfLines(searchBox, aceFinder)
                val releasedEnd: AceKeyCommand = ShowEndOfLines(searchBox, aceFinder)
                releasedHome.addListener(showJumpObserver)
                releasedEnd.addListener(showJumpObserver)
                searchBox.addPreProcessReleaseKey(KeyEvent.VK_HOME, releasedHome)
                searchBox.addPreProcessReleaseKey(KeyEvent.VK_LEFT, releasedHome)
                searchBox.addPreProcessReleaseKey(KeyEvent.VK_RIGHT, releasedEnd)
                searchBox.addPreProcessReleaseKey(KeyEvent.VK_END, releasedEnd)

                val releasedUp: AceKeyCommand = ShowFirstCharOfLines(searchBox, aceFinder)
                releasedUp.addListener(showJumpObserver)
                searchBox.addPreProcessReleaseKey(KeyEvent.VK_UP, releasedUp)

                val pressedBackspace: AceKeyCommand = ClearResults(searchBox, aceCanvas)
                searchBox.addPreProcessPressedKey(KeyEvent.VK_BACK_SPACE, pressedBackspace)

                val pressedSpace: AceKeyCommand = ShowWhiteSpace(searchBox, aceFinder)
                pressedSpace.addListener(showJumpObserver)
                searchBox.addPreProcessPressedKey(KeyEvent.VK_SPACE, pressedSpace)

                val defaultKeyCommand: DefaultKeyCommand? = DefaultKeyCommand(searchBox, aceFinder, aceJumper, textAndOffsetHash)
                defaultKeyCommand?.addListener(showJumpObserver)
                searchBox.defaultKeyCommand = defaultKeyCommand

                // todo: refactor - edge cases...
                val pressedSemi: AceKeyCommand = ChangeToBeforeCharMode(searchBox, aceFinder)
                pressedSemi.addListener(showJumpObserver)
                searchBox.addPreProcessPressedKey(KeyEvent.VK_SEMICOLON, pressedSemi)

                // todo: refactor - edge cases...
                val pressedQuote: AceKeyCommand = ChangeToAfterCharMode(searchBox, aceFinder)
                pressedQuote.addListener(showJumpObserver)
                searchBox.addPreProcessPressedKey(KeyEvent.VK_QUOTE, pressedQuote)
            }

            setupSearchBoxKeys()
            searchBox.setFont(font)
            val popupBuilder: ComponentPopupBuilder? = JBPopupFactory.getInstance()?.createComponentPopupBuilder(searchBox: JComponent, searchBox)
            popupBuilder?.setCancelKeyEnabled(true)
            val popup = (popupBuilder?.createPopup() as AbstractPopup?)
            popup?.show(guessBestLocation(editor))
            val width = searchBox.getFontMetrics(font)?.stringWidth("w")
            var dimension: Dimension? = null
            if(width != null){
                dimension = Dimension(width * 2, (editor.getLineHeight()))
                if (SystemInfo.isMac) {
                    dimension?.setSize(dimension!!.width * 2, dimension!!.height * 2)
                }
            }


            popup?.setSize(dimension as Dimension)
            searchBox.popupContainer = popup
            searchBox.setSize(dimension as Dimension)
            searchBox.setFocusable(true)
            searchBox.addFocusListener(object : FocusListener {
                public override fun focusGained(e: FocusEvent) {
                    addAceCanvas()
                }
                public override fun focusLost(e: FocusEvent) {
                    exit()
                }
            })

        }

        configureSearchBox()

        fun configureAceCanvas() {
            aceCanvas.setFont(font)
            aceCanvas.lineHeight = editor.getLineHeight()
            aceCanvas.lineSpacing = scheme?.getLineSpacing()!!
            aceCanvas.colorPair = Pair<Color?, Color?>(scheme?.getDefaultBackground(), scheme?.getDefaultForeground())
        }

        configureAceCanvas()


        ApplicationManager.getApplication()?.invokeLater(object:Runnable{
            public override fun run() {
                IdeFocusManager.getInstance(project)?.requestFocus(searchBox, true)
            }

        });
    }

}