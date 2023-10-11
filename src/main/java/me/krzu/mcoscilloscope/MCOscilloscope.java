package me.krzu.mcoscilloscope;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.sound.sampled.LineUnavailableException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.Oscilloscope;
import be.tarsos.dsp.Oscilloscope.OscilloscopeEventHandler;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class MCOscilloscope implements OscilloscopeEventHandler {
    private final MCOscilloscopePlugin plugin;

    private final Set<BlockDisplay> blocks1 = new HashSet<>();
    private final Set<BlockDisplay> blocks2 = new HashSet<>();
    private AudioDispatcher dispatcher;
    private World world;
    private Location loc;

    private BlockData block1;
    private BlockData block2;

    public MCOscilloscope(MCOscilloscopePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleEvent(float[] buffer, AudioEvent event) {
        byte[] bytes = event.getByteBuffer();
        float step = (float) 20_460 / bytes.length;

        this.plugin.newChain()
                .sync(() -> {
                    this.handleDisplay(blocks1, bytes, step, 6);
                    this.handleDisplay(blocks2, bytes, step, 2);
                })
                .execute();
    }

    private void handleDisplay(Set<BlockDisplay> blocks, byte[] bytes, float step, int yOffset) {
        if (blocks.isEmpty()) {
            for (int i = 1; i < bytes.length; i += 2) {
                float percent = bytes[i] / 128.0f;
                float x = (i * step) / ((float) 20_460 / 200);
                float y = 30.0f + ((percent * 100) / yOffset);

                BlockDisplay bd = this.world.spawn(
                        this.loc.clone().add(x, y, 0), BlockDisplay.class, block -> {
                            block.setBlock(yOffset == 2 ? this.block1 : this.block2);
                            block.setViewRange(100.0f);
                            block.setTransformation(
                                    new Transformation(
                                            new Vector3f(),
                                            new AxisAngle4f(),
                                            new Vector3f(2, 0.5f, 10),
                                            new AxisAngle4f()
                                    )
                            );
                        });

                blocks.add(bd);
            }
        } else {
            int i = 1;
            for (BlockDisplay block : blocks) {
                float percent = bytes[i] / 128.0f;
                float x = (i * step) / ((float) 20_460 / 200);
                float y = 30.0f + ((percent * 100) / yOffset);

                block.teleport(this.loc.clone().add(x, y, 0));
                i += 2;
            }
        }
    }

    public void start(Player player) {
        player.sendMessage("Starting oscilloscope...");

        FileConfiguration config = this.plugin.getConfig();
        Location loc = player.getLocation();
        this.loc = loc;
        this.world = loc.getWorld();
        this.block1 = Material.valueOf(config.getString("blocks.1", "BLACK_CONCRETE")).createBlockData();
        this.block2 = Material.valueOf(config.getString("blocks.2", "LIME_CONCRETE")).createBlockData();

        this.plugin.newChain()
                .async(() -> {
                    player.sendMessage("Oscilloscope started!");
                    File audio = new File(this.plugin.getDataFolder(), config.getString("audio-file", "audio.mp3"));
                    dispatcher = AudioDispatcherFactory.fromPipe(audio.getAbsolutePath(), 44100, 1024, 0);
                    dispatcher.addAudioProcessor(new Oscilloscope(this));
                    try {
                        dispatcher.addAudioProcessor(new AudioPlayer(dispatcher.getFormat()));
                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                    }
                    dispatcher.run();
                    this.stop(null);
                    player.sendMessage("Oscilloscope ended!");
                })
                .execute();
    }

    public void stop(@Nullable Player player) {
        if (player != null) {
            player.sendMessage("Oscilloscope stopped!");
        }

        this.plugin.newChain()
                .sync(() -> {
                    if (!blocks1.isEmpty()) {
                        for (BlockDisplay block : blocks1) {
                            block.remove();
                        }
                        blocks1.clear();
                    }

                    if (!blocks2.isEmpty()) {
                        for (BlockDisplay block : blocks2) {
                            block.remove();
                        }
                        blocks2.clear();
                    }
                })
                .execute();

        if (dispatcher != null) {
            dispatcher.stop();
        }
    }
}
