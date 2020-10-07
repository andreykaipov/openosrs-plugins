package com.kaipov.plugins

import com.kaipov.common.extensions.client.hasBankOpen
import com.google.inject.Provides
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_0
import java.awt.event.KeyEvent.VK_BACK_QUOTE
import javax.inject.Inject
import net.runelite.api.Client
import net.runelite.api.ScriptID
import net.runelite.api.VarClientStr
import net.runelite.api.events.WidgetLoaded
import net.runelite.api.widgets.WidgetID
import net.runelite.api.widgets.WidgetInfo
import net.runelite.client.callback.ClientThread
import net.runelite.client.config.ConfigGroup
import net.runelite.client.config.ConfigManager
import net.runelite.client.eventbus.Subscribe
import net.runelite.client.input.KeyListener
import net.runelite.client.input.KeyManager
import net.runelite.client.plugins.Plugin
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import org.pf4j.Extension

@ConfigGroup("com.kaipov.plugins.bankhotkeys")
interface Config : net.runelite.client.config.Config {}

@Extension
@PluginDescriptor(name = "Bank Hotkeys", description = "aa", type = PluginType.UTILITY)
class BankHotkeys : Plugin(), KeyListener {

    @Inject
    lateinit var client: Client

    @Inject
    lateinit var clientThread: ClientThread

    @Inject
    lateinit var keyManager: KeyManager

    @Inject
    lateinit var config: Config

    @Provides
    fun config(man: ConfigManager): Config = man.getConfig(Config::class.java)

    override fun keyTyped(e: KeyEvent?) {}
    override fun keyReleased(e: KeyEvent?) {}

    /**
     * TODO make it configurable - backtick opens up 0
     *
     * Script 274 (bankmain_init) https://github.com/RuneStar/cs2-scripts/blob/master/scripts/%5Bclientscript,bankmain_init%5D.cs2
     * references script 504 (bankmain_switchtab), so we can find out what
     * arguments script 504 uses based off the args for script 274.
     *
     * So, how do we find the arguments for script 274? Trial and error.
     *
     * Just keep calling one of the four methods on a widget:
     *
     * Object[] getOnOpListener()
     * Object[] getOnKeyListener()
     * Object[] getOnLoadListener()
     * Object[] getOnInvTransmit()
     *
     * If nothing gets printed out, iterate to its parents. Script 274 and its
     * args were found from `client.getWidget(BANK_CONTAINER)?.onLoadListener`.
     * Print that boy out, and we'll find the script ID to be 274 with 33 more
     * arguments, corresponding with the 33 args from RuneStar's decompiled cs2
     * script above. Just match them up.
     */
    override fun keyPressed(e: KeyEvent) {
        if (!client.hasBankOpen()) {
            keyManager.unregisterKeyListener(this)
            return
        }

        val tab = if (e.keyCode == VK_BACK_QUOTE) 0 else e.keyCode - VK_0
        if (!(0..9).contains(tab)) return

        val ogText = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT)

        clientThread.invoke(Runnable {
            resetChatbox(ogText)

            val w = client.getWidget(WidgetInfo.BANK_CONTAINER) ?: return@Runnable
            if (w.isHidden) return@Runnable
            val components = w.onLoadListener.drop(1) // the first arg is just the script id 274

            client.runScript(
                504,
                components[0],
                components[2],
                components[7],
                components[8],
                components[9],
                components[16],
                components[17],
                components[15],
                components[18],
                components[19],
                components[20],
                components[21],
                1,
                tab,
                components[28],
                components[29],
                components[30],
                components[31],
                components[32],
            )
        })

        e.consume()
    }

    /**
     * Redraws the user's chatbox.
     * Must be ran from a client thread, i.e. clientThread.invoke(Runnable { ... })
     */
    private fun resetChatbox(text: String = "") {
        assert(client.isClientThread)
        client.setVar(VarClientStr.CHATBOX_TYPED_TEXT, text)
        client.runScript(ScriptID.CHAT_PROMPT_INIT)
    }

    /**
     * Unfortunately there is no WidgetUnloaded event (and it looks like
     * WidgetHiddenChanged doesn't actually send events on already unloaded
     * widgets), so the listener will unregister itself when it finds out it
     * no longer has the bank interface open.
     */
    @Subscribe
    private fun handle(event: WidgetLoaded) {
        if (event.groupId == WidgetID.BANK_GROUP_ID) {
            keyManager.registerKeyListener(this)
        }
    }
}
