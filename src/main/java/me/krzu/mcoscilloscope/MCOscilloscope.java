package me.krzu.mcoscilloscope;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.LineUnavailableException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

  private AudioDispatcher dispatcher;

  private Set<BlockDisplay> blocks1;
  private Set<BlockDisplay> blocks2;
  
  public MCOscilloscope(MCOscilloscopePlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void handleEvent(float[] buffer, AudioEvent event) {
    byte[] bytes = event.getByteBuffer();
    float step = 20_480 / bytes.length;

    this.plugin.newChain()
      .sync(() -> {
          World world = Bukkit.getWorld("world");

          if (blocks1 == null || blocks1.isEmpty()) {
            if (blocks1 == null) {
              blocks1 = new HashSet<>();
            }

            for (int i = 1; i < bytes.length; i += 2) {
              float percent = bytes[i] / 128.0f;
              float x = (float) (i * step) / (20_480 / 150);
              float y = (float) 10.0f + ((percent * 60) / 2);

              
              BlockDisplay bd = world.spawn(
                new Location(world, x, y, 0), BlockDisplay.class, block -> {
                  block.setBlock(Material.ORANGE_CONCRETE.createBlockData());
                  block.setViewRange(60.0f);
                  block.setTransformation(
                    new Transformation(
                      new Vector3f(),
                      new AxisAngle4f(),
                      new Vector3f(1, 1, 5),
                      new AxisAngle4f()
                    )
                  );
                });
              
              blocks1.add(bd);
            }
          } else {
            int i = 1;
            for (BlockDisplay block : blocks1) {
              float percent = bytes[i] / 128.0f;
              float x = (float) (i * step) / (20_480 / 150);
              float y = (float) 10.0f + ((percent * 60) / 2);
      
              block.teleport(new Location(world, x, y, 0));
              i += 2;
            }
          }

          if (blocks2 == null || blocks1.isEmpty()) {
            if (blocks2 == null) {
              blocks2 = new HashSet<>();
            }

            for (int i = 1; i < bytes.length; i += 2) {
              float percent = bytes[i] / 128.0f;
              float x = (float) (i * step) / (20_480 / 150);
              float y = (float) 10.0f + ((percent * 60) / 4);

              BlockDisplay bd = world.spawn(
                new Location(world, x, y, 0), BlockDisplay.class, block -> {
                  block.setBlock(Material.CYAN_CONCRETE.createBlockData());
                  block.setViewRange(60.0f);
                  block.setTransformation(
                    new Transformation(
                      new Vector3f(),
                      new AxisAngle4f(),
                      new Vector3f(1, 1, 5),
                      new AxisAngle4f()
                    )
                  );
                });
              
              blocks2.add(bd);
            }
          } else {
            int i = 1;
            for (BlockDisplay block : blocks2) {
              float percent = bytes[i] / 128.0f;
              float x = (float) (i * step) / (20_480 / 150);
              float y = (float) 10.0f + ((percent * 60) / 4);
      
              block.teleport(new Location(world, x, y, 0));
              i += 2;
            }
          }
        }).execute();
  }

  public void start(Player player) {
    player.sendMessage("Starting oscilloscope...");

    this.plugin.newChain()
        .async(() -> {
          player.sendMessage("Oscilloscope started!");
          File audio = new File(this.plugin.getDataFolder(), "audio.mp3");
          dispatcher = AudioDispatcherFactory.fromPipe(audio.getAbsolutePath(), 44100, 1024, 0);
          dispatcher.addAudioProcessor(new Oscilloscope(this));
          try {
            dispatcher.addAudioProcessor(new AudioPlayer(dispatcher.getFormat()));
          } catch (LineUnavailableException e) {
            e.printStackTrace();
          }
          dispatcher.run();
        })
        .execute();
  }

  public void stop(Player player) {
    player.sendMessage("Oscilloscope stopped!");

    if (blocks1 != null) {
      for (BlockDisplay block : blocks1) {
        block.remove();
      }
      blocks1.clear();
      blocks1 = null;
    }

    if (blocks2 != null) {
      for (BlockDisplay block : blocks2) {
        block.remove();
      }
      blocks2.clear();
      blocks2 = null;
    }

    if (dispatcher != null) {
      dispatcher.stop();
    }
  }
}
