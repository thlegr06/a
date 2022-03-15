/**

Notify
Copyright (C) 2022  BanDev

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

package extensions

import EnvironmentVariables
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.components.ComponentContainer
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.publicButton
import com.kotlindiscord.kord.extensions.components.types.emoji
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.edit
import data.Datastore
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Guild
import dev.kord.rest.Image
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import extensions.voicestateupdate.Action
import io.github.qbosst.kordex.commands.hybrid.ephemeralHybridCommand

class ConfigureHybridExtension: Extension() {
    override val name = "configure"

    override suspend fun setup() {
        ephemeralHybridCommand {
            name = "Configure"
            description = "Configure a server's preferences"

            check { isNotBot() }
            check { hasPermission(Permission.ManageGuild) }

            action {
                val guild = member?.getGuild()!!
                respond {
                    content = "I have sent you the configuration options for this server via DM."
                }
                member?.getDmChannel()?.createMessage {
                    embed {
                        author {
                            name = "${guild.name} configuration"
                            icon = guild.getIconUrl(Image.Format.PNG)
                        }
                        color = Color(EnvironmentVariables.accentColor()[0], EnvironmentVariables.accentColor()[1], EnvironmentVariables.accentColor()[2])
                        actionList.forEach { action ->
                            field("${action.name.lowercase().replaceFirstChar { it.titlecase()} } ${if (getActionToggle(action, member!!.getGuild())) Emojis.whiteCheckMark.unicode else Emojis.x.unicode}")
                        }
                    }
                    components {
                        member?.let {
                            actionList.forEach { action ->
                                publicActionButton(action, guild)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun ComponentContainer.publicActionButton(action: Action, guild: Guild) {
        publicButton {
            label = action.name.lowercase().replaceFirstChar { it.titlecase() }
            style = ButtonStyle.Secondary
            emoji(action.emoji.unicode)
            deferredAck = true
            action {
                Datastore.GuildPrefsCollection.update(guild, action, !getActionToggle(action, guild))
                edit {
                    this.embed {
                        author {
                            name = "${guild.name} server configuration"
                            icon = guild.getIconUrl(Image.Format.PNG)
                        }
                        color = Color(EnvironmentVariables.accentColor()[0], EnvironmentVariables.accentColor()[1], EnvironmentVariables.accentColor()[2])
                        actionList.forEach { action ->
                            field("${action.name.lowercase().replaceFirstChar { it.titlecase()} } ${if (getActionToggle(action, guild)) Emojis.whiteCheckMark.unicode else Emojis.x.unicode}")
                        }
                    }
                }
            }
        }
    }

    private val actionList = listOf(
        Action.JOIN,
        Action.LEAVE,
        Action.SWITCH,
        Action.STREAM,
        Action.VIDEO
    )

    private suspend fun getActionToggle(action: Action, guild: Guild): Boolean {
        val guildPrefs = Datastore.GuildPrefsCollection.get(guild)
        return when (action) {
            Action.JOIN -> guildPrefs.join
            Action.LEAVE -> guildPrefs.leave
            Action.SWITCH -> guildPrefs.switch
            Action.STREAM -> guildPrefs.stream
            Action.VIDEO -> guildPrefs.video
            Action.UNKNOWN -> error("Unknown")
        }
    }
}