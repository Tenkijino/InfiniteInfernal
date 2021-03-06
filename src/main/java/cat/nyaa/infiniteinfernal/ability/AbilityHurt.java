package cat.nyaa.infiniteinfernal.ability;

import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public interface AbilityHurt {
    default void onHurt(IMob mob, EntityDamageEvent event){}
    default void onHurtByPlayer(IMob mob, EntityDamageByEntityEvent event){onHurt(mob, event);}
    default void onHurtByNonPlayer(IMob mob, EntityDamageByEntityEvent event){onHurt(mob, event);}
}
