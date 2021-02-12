package net.minelc.minelc_pvpgames.listeners;

import com.minelc.CORE.Controller.Database;
import com.minelc.CORE.Controller.Jugador;
import com.minelc.CORE.CoreMain;
import com.minelc.CORE.Utils.Util;
import net.minelc.minelc_pvpgames.MineLC_PvPGames;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerListener implements Listener {
    @EventHandler
    public void onLogin(final AsyncPlayerPreLoginEvent e) {
        String p = e.getName();
        Jugador j = Jugador.getJugador(p);

        Database.loadPlayerSV_PVPGAMES_SYNC(j);
        Database.loadPlayerRank_SYNC(j);
        Database.loadPlayerCoins_SYNC(j);
    }

    /*
    * Según este enlace
    * https://www.spigotmc.org/threads/priority-of-plugins.96841/#post-1060774
    * La prioridad LOWEST es la que primero ocurre.
    *
    * Aquí se entregan los permisos al jugador, y un título + sonidito de MineLC
    * */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Jugador j = Jugador.getJugador(p);
        j.setBukkitPlayer(p);

        enviarTitulosIniciales(e.getPlayer());
        loadPlayerPermission(p);
        // setScoreboard(j);
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.5F, 0.5F);
        Bukkit.getLogger().info("[Debug] A este punto el usuario " + p.getName() + " debió haberse cargado.");
    }

    private void loadPlayerPermission(Player p) {
        Jugador j = Jugador.getJugador(p);

        if (!j.isHideRank()) {
            switch (j.getRank()) {
                case VIP:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.vip", true);
                    break;
                case SVIP:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.svip", true);
                    break;
                case ELITE:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.elite", true);
                    break;
                case RUBY:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.ruby", true);
                    break;
                case YOUTUBER:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.youtuber", true);
                    break;
                case MINIYT:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.miniyt", true);
                    break;
                case BUILDER:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.builder", true);
                    break;
                case AYUDANTE:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.ayudante", true);
                    break;
                case MOD:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.mod", true);
                    break;
                case ADMIN:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.admin", true);
                    break;
                case OWNER:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.owner", true);
                    break;
                default:
                    p.addAttachment(CoreMain.getInstance(), "pgm.group.default", true);
                    break;
            }
        } else {
            if (j.is_VIP()) {
                p.addAttachment(CoreMain.getInstance(), "pgm.premium", true);
            }
        }
    }

    public void enviarTitulosIniciales(Player p) {
        String titulos = "&aPvPGames";
        titulos = ChatColor.translateAlternateColorCodes('&', titulos);

        String subtitulos = "&6www.minelc.net";
        subtitulos = ChatColor.translateAlternateColorCodes('&', subtitulos);
        // p.sendTitle(titulos, subtitulos, 20, 60, 20);
        Util.sendTitle(p, 20, 60, 20, titulos, subtitulos);
    }
}
