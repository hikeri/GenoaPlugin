package dev.projectearth.genoa_plugin.commands;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import dev.projectearth.genoa_plugin.GenoaPlugin;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.TextFormat;


/**
 * A basic loader command for buildplates.
 *
 * @author rtm516
 */
public class BuildplateCommand extends Command {

    public BuildplateCommand() {
        super("buildplate", CommandData.builder("buildplate")
                .setDescription("Load a buildplate")
                .setUsageMessage("/buildplate <action>")
                .setPermissions("genoa.command.buildplate")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("id", CommandParamType.STRING, false)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        Level buildplateLevel = GenoaPlugin.get().registerBuildplate(args[0]);

        if (sender instanceof Player) {
            ((Player) sender).teleportImmediate(buildplateLevel.getSpawnLocation());
        } else {
            for (Player player : CloudServer.getInstance().getOnlinePlayers().values()) {
                player.teleportImmediate(buildplateLevel.getSpawnLocation());
            }
        }

        return true;
    }
}
