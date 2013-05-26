package com.johnlindquist.acejump.ui

import com.johnlindquist.acejump.keycommands.AceKeyCommand
import java.util.HashMap
import javax.swing.JTextField
import com.intellij.ui.popup.AbstractPopup
import java.awt.Graphics
import java.awt.event.KeyEvent
import javax.swing.text.BadLocationException

public class SearchBox: JTextField() {
    val preProcessKeyReleasedMap = HashMap<Int, AceKeyCommand>()
    val preProcessKeyPressedMap = HashMap<Int, AceKeyCommand>()

    var isSearchEnabled = true
        get(){
            return $isSearchEnabled && getText()?.length() == 1
        }

    var popupContainer: AbstractPopup? = null
    var defaultKeyCommand: AceKeyCommand? = null


    public override fun requestFocus() {
        setTransferHandler(null);
        super<JTextField>.requestFocus()
    }
    override fun paintBorder(e: Graphics?) {
    }


    //todo: I need to really rethink this entire approach
    override fun processKeyEvent(e: KeyEvent) {

        if (getText()?.length() == 0) {
            //todo: rethink the "isSearchEnabled" state approach. Works great now, could be cleaner
            isSearchEnabled = true
        }


        var aceKeyCommand: AceKeyCommand? = null
        if (e.getID() == KeyEvent.KEY_RELEASED) {
            aceKeyCommand = preProcessKeyReleasedMap.get(e.getKeyCode())
        }

        if (e.getID() == KeyEvent.KEY_PRESSED) {

            aceKeyCommand = preProcessKeyPressedMap.get(e.getKeyCode())
            //prevent "alt" from triggering menu items
            e.consume()
        }


        if (aceKeyCommand != null) {
            aceKeyCommand?.execute(e)
            return
        }

        super.processKeyEvent(e)

        if (e.getID() != KeyEvent.KEY_TYPED) {
            return
        }


        if (defaultKeyCommand != null && e.isConsumed()){
            println("### execute defaultKeyCommand")
            defaultKeyCommand?.execute(e)
        }

        if (getText()?.length() == 2) {
            println("### 2 chars: " + getText())
            try{
                setText(getText(0, 1))
            }
            catch (e1: BadLocationException) {
                e1.printStackTrace()
            }
        }
    }

    fun forceSpaceChar() {
        setText(" ")
        disableSearch()
    }

    fun disableSearch() {
        isSearchEnabled = false
    }

    fun addPreProcessReleaseKey(key: Int, keyCommand: AceKeyCommand) {
        preProcessKeyReleasedMap.put(key, keyCommand);
    }

    fun addPreProcessPressedKey(key: Int, keyCommand: AceKeyCommand) {
        preProcessKeyPressedMap.put(key, keyCommand);
    }

}