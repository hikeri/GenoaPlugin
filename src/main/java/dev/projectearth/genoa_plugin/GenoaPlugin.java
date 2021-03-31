package dev.projectearth.genoa_plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.nukkitx.protocol.genoa.packet.GenoaItemParticlePacket;
import dev.projectearth.genoa_plugin.commands.BuildplateCommand;
import dev.projectearth.genoa_plugin.commands.SummonCommand;
import dev.projectearth.genoa_plugin.commands.TestEntCommand;
import dev.projectearth.genoa_plugin.entities.GenoaEntityLoader;
import dev.projectearth.genoa_plugin.generators.BuildplateGenerator;
import dev.projectearth.genoa_plugin.utils.Buildplate;
import dev.projectearth.genoa_plugin.utils.BuildplateEntity;
import lombok.Getter;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.block.BlockBreakEvent;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.storage.StorageIds;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;
import org.cloudburstmc.server.registry.CommandRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.registry.GeneratorRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.slf4j.Logger;
import org.cloudburstmc.api.plugin.Plugin;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.event.player.PlayerJoinEvent;
import org.cloudburstmc.server.event.server.ServerInitializationEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(id = "GenoaPlugin", name = "Genoa Plugin", version = "1.0.0")
public class GenoaPlugin implements PluginContainer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static GenoaPlugin INSTANCE;

    @Getter
    private final Logger logger;
    @Getter
    private final PluginDescription description;
    @Getter
    private final Path dataDirectory;
    private final CloudServer server;

    @Getter
    private Map<String, Buildplate> buildplates = new HashMap<>();

    @Inject
    private GenoaPlugin(Logger logger, PluginDescription description, Path dataDirectory) {
        this.logger = logger;
        this.description = description;
        this.dataDirectory = dataDirectory;

        this.server = CloudServer.getInstance();
        INSTANCE = this;
    }

    @Listener
    public void onInitialization(ServerInitializationEvent event) {
        this.logger.info("Genoa plugin loading...");
        GeneratorRegistry.get().register(BuildplateGenerator.ID, BuildplateGenerator::new, 0);
        CommandRegistry.get().register(this, new BuildplateCommand());
        CommandRegistry.get().register(this, new TestEntCommand());
        CommandRegistry.get().register(this, new SummonCommand());
        GenoaEntityLoader.load();
        this.logger.info("Genoa plugin has loaded!");
    }

    @Listener
    public void onBlockBreak(BlockBreakEvent event) {
        GenoaItemParticlePacket packet = new GenoaItemParticlePacket();
        packet.setPosition(event.getBlock().getPosition().toFloat().add(0.5, 0.5, 0.5));
        packet.setParticleId(5); // TODO: Find out if 5 always works, or if we need multiple particles
        packet.setDimensionId(1);
        packet.setUniqueEntityId(event.getPlayer().getUniqueId());
        event.getPlayer().sendPacket(packet);
        //this.logger.info("Sent block breaking packet!");
    }

    public Level registerBuildplate(String buildplateId) {
        try {
            this.logger.info("Downloading buildplate " + buildplateId + "...");
            File buildplateFile = new File(buildplateId + ".json");
            if (!buildplateFile.exists()) {
                // TODO: Download from server
                return null;
            }

            this.logger.info("Loading buildplate " + buildplateId + "...");
            Buildplate buildplate = OBJECT_MAPPER.readValue(buildplateFile, Buildplate.class);
            buildplates.put(buildplateId, buildplate);

            this.logger.info("Creating level for buildplate " + buildplateId + "...");
            Level buildplateLevel = server.loadLevel()
                    .id(buildplateId)
                    .seed(0)
                    .generator(BuildplateGenerator.ID)
                    .generatorOptions(buildplateId)
                    .storage(StorageIds.LEVELDB)
                    .load()
                    .get();

            // Insert default rules
            buildplateLevel.getGameRules().putAll(CloudGameRuleRegistry.get().getDefaultRules());

            // Change a few to disable features
            buildplateLevel.getGameRules().put(GameRules.DO_DAYLIGHT_CYCLE, false);
            buildplateLevel.getGameRules().put(GameRules.DO_WEATHER_CYCLE, false);


            if (buildplate.getResult().getBuildplateData().getModel().getEntities() != null) {
                for (BuildplateEntity entity : buildplate.getResult().getBuildplateData().getModel().getEntities()) {
                    Entity ent = EntityRegistry.get().newEntity(EntityRegistry.get().getEntityType(Identifier.fromString(entity.getName())), Location.from(entity.getPosition(), buildplateLevel));
                    ent.setPosition(entity.getPosition());
                    ent.setRotation(entity.getRotation().getX(), entity.getRotation().getY());
                    logger.info("Spawning " + ent.getName() + " at " + entity.getPosition() + " for buildplate " + buildplateId);
                    //ent.spawnToAll();
                    buildplateLevel.addEntity(ent);
                }
            }

            return buildplateLevel;
        } catch (InterruptedException | ExecutionException | IOException e) {
            logger.error("Something went wrong loading buildplate '" + buildplateId + "':", e);
        }

        return null;
    }

    @Listener
    public void onJoin(PlayerJoinEvent event) {
        // TODO: Load the buildplate for the user or kick them
    }

    public static GenoaPlugin get() {
        return INSTANCE;
    }

    @Override
    public Object getPlugin() {
        return this;
    }
}
