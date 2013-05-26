package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

public class ShowWhiteSpace(val searchBox: SearchBox, val aceFinder: AceFinder): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        aceFinder.addResultsReadyListener(object :ChangeListener{
            public override fun stateChanged(e: ChangeEvent) {
                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
            }
        })

        aceFinder.findText(AceFinder.WHITE_SPACE, true)
        searchBox.forceSpaceChar()
    }
}