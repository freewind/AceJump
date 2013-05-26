package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.AceJumper
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import java.util.HashMap
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import java.awt.Color
import com.intellij.openapi.wm.ToolWindow.Border
import java.awt.Cursor
import javax.swing.BorderFactory

public class ChangeToAfterCharMode(val searchBox: SearchBox, val aceFinder: AceFinder): AceKeyCommand() {


    override fun execute(keyEvent: KeyEvent) {
        aceFinder.addResultsReadyListener(object: ChangeListener{
            public override fun stateChanged(e: ChangeEvent) {
                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
            }
        })

        if(keyEvent.isMetaDown() || keyEvent.isControlDown()){
            if(aceFinder.isAfterCharMode == false) {
                aceFinder.isAfterCharMode = true
                searchBox.setForeground(Color.decode("336699"))
            } else {
                if(aceFinder.isTargetMode){
                    aceFinder.isTargetMode = false
                    searchBox.setBackground(Color.WHITE)
                }else{
                    aceFinder.isTargetMode = true
                    searchBox.setBackground(Color.RED)
                }
            }
        }
    }
}