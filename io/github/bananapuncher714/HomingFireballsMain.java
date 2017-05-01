package io.github.bananapuncher714;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class HomingFireballsMain extends JavaPlugin implements Listener {
	private boolean fireball, snowball, arrow, smallfireball, egg, potion, targetCreative;
	
	private boolean usePermissions = false;
	
	private double accuracy = .7;
	private int degree = 45;
	private double distance = -1;
	
	private boolean mobsHome = true;
	private boolean dispensers = true;
	
	private boolean homeOnPlayers = true;
	private boolean homeOnMobs = true;
	
	private double LS;
	private HashSet< Player > shooters = new HashSet< Player >();
	private HashMap< Projectile, LivingEntity > fireballs = new HashMap< Projectile, LivingEntity >();
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		loadHFConfig();
		LS = Math.cos( Math.toRadians( degree ) );
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				if ( !fireballs.isEmpty() ) {
					for ( Iterator< Projectile > i = fireballs.keySet().iterator(); i.hasNext(); ) {
						Projectile f = i.next();
						home( f );
					}
				}
				// TODO Auto-generated method stub
			}
		}, 1, 1 );
	}
	
	private void loadHFConfig() {
		// TODO Auto-generated method stub
		try {
			FileConfiguration config = getConfig();
			distance = config.getDouble( "max-range" );
			degree = config.getInt( "degree" );
			accuracy = config.getDouble( "accuracy-percent" );
			fireball = config.getBoolean( "ghast-fireball" );
			snowball = config.getBoolean( "snowball" );
			arrow = config.getBoolean( "arrow" );
			smallfireball = config.getBoolean( "blaze-fireball" );
			egg = config.getBoolean( "egg" );
			potion = config.getBoolean( "potion" );
			mobsHome = config.getBoolean( "mobs-can-aimbot" );
			dispensers = config.getBoolean( "dispensers-can-home" );
			homeOnMobs = config.getBoolean( "home-on-mobs" );
			homeOnPlayers = config.getBoolean( "home-on-players" );
			usePermissions = config.getBoolean( "use-permissions" );
			targetCreative = config.getBoolean( "target-creative-players" );
		} catch ( Exception e ) {
			getLogger().info( "There has been a problem with the configuration. Assuming default values." );
			distance = -1;
			degree = 45;
			accuracy = .7;
			fireball = true;
			snowball = false;
			arrow = true;
			smallfireball = true;
			egg = false;
			potion = false;
			targetCreative = false;
			mobsHome = true;
			dispensers = true;
			homeOnMobs = true;
			homeOnPlayers = true;
			usePermissions = false;
		}
	}

	@Override
	public void onDisable() {
		shooters.clear();
		fireballs.clear();
	}
	
	public void home( Projectile f ) {
		LivingEntity p = fireballs.get( f );
		if ( p instanceof Player && !( ( Player ) p ).isOnline() ) {
			fireballs.remove( f );
		}
		if ( p.getWorld() == f.getWorld() && !f.isDead()) {
			Location floc = f.getLocation();
			Location ploc = p.getEyeLocation();
			double speed = f.getVelocity().length();
			f.setVelocity( f.getVelocity().add( ploc.subtract( floc ).toVector().normalize().multiply( accuracy * speed ) ).normalize().multiply( speed ) );
		} else {
			fireballs.remove( f );
		}
	}
	
	public void showHelp( CommandSender s ) {
		s.sendMessage( ChatColor.AQUA + "Homing Fireballs v1.6 Help Page" + ChatColor.RESET );
		s.sendMessage( ChatColor.GREEN + "/homingfireballs help - Displays the help page" );
		s.sendMessage( ChatColor.GREEN + "/homingfireballs - Enables or disables homing arrows for you" + ChatColor.RESET );
		s.sendMessage( ChatColor.GREEN + "/homingfireballs reload - Reloads the config" + ChatColor.RESET );
	}
	
	@EventHandler
	public void onPlayerQuitEvent( PlayerQuitEvent e ) {
		if ( shooters.contains( e.getPlayer() ) ) shooters.remove( e );
	}
	
	@EventHandler
	public void onProjectileLaunchEvent( ProjectileLaunchEvent e ) {
		Projectile ent = e.getEntity();
		ProjectileSource shooter = ent.getShooter();
		if ( ( shooter instanceof Player && !shooters.contains( shooter ) || ( !mobsHome && !( shooter instanceof Player ) && !( shooter instanceof BlockProjectileSource ) ) || ( shooter instanceof BlockProjectileSource && !dispensers ) ) ) {
			return;
		}
		if ( ( ent instanceof Fireball && fireball ) || ( ent instanceof SmallFireball && smallfireball ) || ( ent instanceof Arrow && arrow ) || ( ent instanceof Snowball && snowball ) || ( ent instanceof Egg && egg ) || ( ent instanceof ThrownPotion && potion ) ) {
			LivingEntity finalist = null;
			if ( homeOnPlayers ) {
				for ( Player p: getServer().getOnlinePlayers() ) {
					if ( ent.getWorld() == p.getWorld() && ( ent.getLocation().distance( p.getLocation() ) < distance || distance == -1 ) && p.hasLineOfSight( ent ) && ( !p.getGameMode().equals( GameMode.CREATIVE ) || targetCreative ) ) {
						finalist = p;
					}
				}
			}
			if ( !( shooter instanceof Player ) && !( shooter instanceof Wither ) && !( shooter instanceof Snowman ) ) {
				if ( finalist != null ) {
					fireballs.put( ent, finalist );
				} else {
					return;
				}
			}
			if ( ( shooter == finalist || ( finalist == null && shooter instanceof Player ) || shooter instanceof Wither ) && homeOnMobs ) {
				for ( int i = 1; i < 100; i++ ) {
					List< Entity > entities = ent.getNearbyEntities( i , i , i );
					for ( Iterator< Entity > it = entities.iterator(); it.hasNext(); ) {
						Entity newent = it.next();
						if ( newent instanceof LivingEntity && newent != shooter && ( ( LivingEntity ) shooter ).hasLineOfSight( newent ) ) {
							Vector fov = newent.getLocation().subtract( ( ( Entity ) shooter ).getLocation() ).toVector().normalize();
							Vector ffov = ent.getVelocity().normalize();
							if ( ( Math.abs( fov.getX() - ffov.getX() ) <= LS && Math.abs( fov.getY() - ffov.getY() ) <= LS && Math.abs( fov.getZ() - ffov.getZ() ) <= LS ) || degree == -1  || shooter instanceof Wither ) {
								fireballs.put( ent, ( LivingEntity ) newent );
								return;
							}
						}
					}
				}
			}
			if ( finalist != null && shooter != finalist ) fireballs.put( ent, finalist );
			return;
		}
	}
	
	@EventHandler
	public void onProjectileHitEvent( ProjectileHitEvent e ) {
		Entity ent = e.getEntity();
		if ( fireballs.containsKey( ent ) ) {
			fireballs.remove( ent );
		}
	}
	
	@Override
	public boolean onCommand( CommandSender s, Command c, String l, String[] a ) {
		if ( c.getName().equalsIgnoreCase( "homingfireballs" ) ) {
			if ( a.length == 0 ) {
				if ( s instanceof Player ) {
					if ( s.hasPermission( "homingfireballs.home" ) || !usePermissions ) {
						if ( shooters.contains( ( Player ) s ) ) {
							shooters.remove( s );
							s.sendMessage( ChatColor.BLUE + "Your projectiles no longer home" + ChatColor.RESET );
							return true;
						} else {
							shooters.add( ( Player ) s );
							s.sendMessage( ChatColor.BLUE + "Your projectiles now home" + ChatColor.RESET );
							return true;
						}
					} else {
						s.sendMessage( ChatColor.RED + "You do not have permission to run this command!" + ChatColor.RESET );
						return false;
					}
				} else {
					s.sendMessage( ChatColor.RED + "You can only run this command as a player!" + ChatColor.RESET );
					showHelp( s );
					return false;
				}
			} else if ( a.length == 1 ) {
				if ( a[ 0 ].equalsIgnoreCase( "help" ) && ( s instanceof Player && ( ( s.hasPermission( "homingfireballs.help" ) || !usePermissions ) ) || !( s instanceof Player ) ) ) {
					showHelp( s );
					return true;
				} else if ( a[ 0 ].equalsIgnoreCase( "reload" ) && ( ( s instanceof Player && s.hasPermission( "homingfireballs.reload" ) || !usePermissions ) || !( s instanceof Player ) ) ) {
					loadHFConfig();
					s.sendMessage( ChatColor.AQUA + "Config Reloaded!" + ChatColor.RESET );
					return true;
				} else {
					s.sendMessage( ChatColor.RED + "That is not a valid option!" + ChatColor.RESET );
					showHelp( s );
					return false;
				}
			} else {
				showHelp( s );
				return false;
			}
		}
		return false;	
	}
}
