package net.minelc.minelc_pvpgames.listeners;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.core.CoreBlockBreakEvent;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.killreward.KillReward;
import tc.oc.pgm.spawns.events.DeathKitApplyEvent;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

import java.util.*;

public class PlayerListener implements Listener {
    public boolean isCoreLeak;
    public List<String> recievedRewardDTC = new ArrayList<>();

    @EventHandler
    public void onLogin(final AsyncPlayerPreLoginEvent e) {
        String p = e.getName();
        Jugador j = Jugador.getJugador(p);

        Bukkit.getScheduler().runTaskAsynchronously(MineLC_PvPGames.getInstance(), () -> {
            Database.loadPlayerRank_SYNC(j);
            Database.loadPlayerSV_PVPGAMES_SYNC(j);
            Database.loadPlayerCoins_SYNC(j);
        });
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

        Bukkit.getLogger().info("[Debug] A este punto el usuario " + p.getName() + " debió haberse cargado.");
        Bukkit.getScheduler().runTaskLater(MineLC_PvPGames.getInstance(), new Runnable() {
            @Override
            public void run() {
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.5F, 0.5F);
            }
        }, 2L);
    }

    @EventHandler
    public void onFinishEvent(MatchFinishEvent e) {
        String game = e.getEventName();
        Collection<Competitor> competitors = e.getMatch().getCompetitors();
        Collection<MatchPlayer> mps = e.getMatch().getParticipants();

        if (Bukkit.getServer().getOnlinePlayers().size() == 1) {
            Bukkit.getLogger().info("¡Partida acabada porque NO HAY JUGADORES SUFICIENTES!");
            return;
        }

        for(Competitor comp : e.getWinners()) {
            for (MatchPlayer mp : mps) {
                if (mp.getCompetitor() == comp) {
                    Player p = mp.getBukkit();
                    Jugador j = Jugador.getJugador(p);
                    p.sendMessage("¡Has ganado!");
                    agregarLCoins(j, "win");

                    j.addPVPG_Stats_Partidas_ganadas(1);
                    Database.savePlayerSV_PVPGAMES(j);

                    Bukkit.getLogger().info("[Debug] EL USUARIO " + mp.getNameLegacy() + " es " + p.getName() + " y ganó con " + comp.getNameLegacy() + "!!!");
                }
            }
            Bukkit.getLogger().info("[Debug] ¡Finalizado evento " + game + " con ganador " + comp.getNameLegacy() + "!");
        }

        for(Competitor comp : competitors) {
            for (MatchPlayer mp : comp.getPlayers()) {
                Player p = mp.getBukkit();
                Jugador j = Jugador.getJugador(p);

                j.addPVPG_Stats_Partidas_jugadas(1);
                Database.savePlayerSV_PVPGAMES(j);
            }
        }

        isCoreLeak = false;
        recievedRewardDTC.clear();
    }

    @EventHandler
    public void onMatchStart(MatchStartEvent e) {
        for (MapTag mt : e.getMatch().getMap().getTags()) {
            if (mt.isGamemode()) {
                String gamemode = mt.getId();
                Bukkit.getLogger().warning("Starting match with gamemode... " + mt.getName() + " and Id " + mt.getId());
                isCoreLeak = gamemode.equalsIgnoreCase("core") || gamemode.equalsIgnoreCase("dtc");
            }
        }
    }


    @EventHandler
    public void onWoolPlace(PlayerWoolPlaceEvent e) {
        Optional<MatchPlayer> mp = e.getPlayer().getPlayer();

        if (mp.isPresent()) {
            Player p = mp.get().getBukkit();
            Jugador j = Jugador.getJugador(p);

            agregarLCoins(j, "place-wool");
            j.addPVPG_Stats_wools_placed(1);
            Database.savePlayerSV_PVPGAMES(j);
        }
    }

    @EventHandler
    public void onCoreBlockBreak(CoreBlockBreakEvent e) {
        Optional<MatchPlayer> mp = e.getPlayer().getPlayer();

        if (mp.isPresent()) {
            Player p = mp.get().getBukkit();
            Jugador j = Jugador.getJugador(p);
            if (isCoreLeak) {
                if(recievedRewardDTC.contains(p.getName())) {
                    Bukkit.getLogger().info("¡No se pudo entregar premio a " + p.getName() + " porque ya recibió con anterioridad!");
                } else {
                    recievedRewardDTC.add(p.getName());
                    agregarLCoins(j, "destroy-monument");
                    j.addPVPG_Stats_monuments_destroyed(1);
                    Database.savePlayerSV_PVPGAMES(j);
                }
            }
        }
    }

    @EventHandler
    public void onCoreLeak(CoreLeakEvent e) {
        ImmutableList<Contribution> mp = e.getCore().getContributions();

        for (Contribution con : mp) {
            if (con.getPercentage() > 0.2) {
                Jugador j = Jugador.getJugador(con.getPlayerState().getNameLegacy());

                agregarLCoins(j, "core-leak");
                j.addPVPG_Stats_cores_leakeds(1);
                Database.savePlayerSV_PVPGAMES(j);
            }
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player muerto = e.getEntity().getPlayer();
        Player asesino = e.getEntity().getKiller();

        if (asesino != null) {
            Jugador ja = Jugador.getJugador(asesino);

            agregarLCoins(ja, "kill");
            ja.addPVPG_Stats_kills(1);
            Database.savePlayerSV_PVPGAMES(ja);
        }

        Jugador jm = Jugador.getJugador(muerto);
        jm.addPVPG_Stats_deaths(1);
        Database.savePlayerSV_PVPGAMES(jm);
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

    /**
     * Agrega LCoins según contexto
     *
     * @param jug Jugador de MineLC
     * @param context Contexto: "kill", "place-wool", "destroy-monument", "core-leak", o "win"
     */
    private void agregarLCoins(Jugador jug, String context) {
        context = context.toLowerCase();

        if(jug.is_RUBY()) {
            switch (context) {
                case "kill":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.kill.ruby", 3));
                    break;
                case "place-wool":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.place-wool.ruby", 5));
                    break;
                case "destroy-monument":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.destroy-monument.ruby", 8));
                    break;
                case "core-leak":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.core-leak.ruby", 8));
                    break;
                case "win":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.win.ruby", 2));
                    break;
            }
        } else if (jug.is_ELITE()) {
            switch (context) {
                case "kill":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.kill.elite", 3));
                    break;
                case "place-wool":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.place-wool.elite", 5));
                    break;
                case "destroy-monument":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.destroy-monument.elite", 8));
                    break;
                case "core-leak":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.core-leak.elite", 8));
                    break;
                case "win":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.win.elite", 2));
                    break;
            }
        } else if (jug.is_SVIP()) {
            switch (context) {
                case "kill":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.kill.svip", 3));
                    break;
                case "place-wool":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.place-wool.svip", 5));
                    break;
                case "destroy-monument":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.destroy-monument.svip", 8));
                    break;
                case "core-leak":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.core-leak.svip", 8));
                    break;
                case "win":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.win.svip", 2));
                    break;
            }
        } else if (jug.is_VIP()) {
            switch (context) {
                case "kill":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.kill.vip", 3));
                    break;
                case "place-wool":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.place-wool.vip", 5));
                    break;
                case "destroy-monument":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.destroy-monument.vip", 8));
                    break;
                case "core-leak":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.core-leak.vip", 8));
                    break;
                case "win":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.win.vip", 2));
                    break;
            }
        } else {
            switch (context) {
                case "kill":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.kill.default", 3));
                    break;
                case "place-wool":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.place-wool.default", 5));
                    break;
                case "destroy-monument":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.destroy-monument.default", 8));
                    break;
                case "core-leak":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.core-leak.default", 8));
                    break;
                case "win":
                    addBalance(jug, MineLC_PvPGames.getInstance().getIntConfig("lcoins.win.default", 1));
                    break;
            }
        }

    }

    private void addBalance(Jugador jug, int x) {
        jug.getBukkitPlayer().playSound(jug.getBukkitPlayer().getLocation(), Sound.NOTE_PLING, 1f, 1.3f);
        jug.getBukkitPlayer().sendMessage(ChatColor.GOLD+"+"+x+" LCoins");
        jug.addLcoins(x);
        Database.savePlayerCoins(jug);
    }
}
