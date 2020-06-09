package cat.nyaa.infiniteinfernal.ui;

import org.bukkit.ChatColor;

public enum PlayerStatus {
    NORMAL(ChatColor.LIGHT_PURPLE), DAMAGED(ChatColor.RED), BUFFED(ChatColor.BLUE), HIT_TARGET(ChatColor.GOLD);

    private ChatColor color;

    PlayerStatus(ChatColor color){
        this.color = color;
    }

    public ChatColor getColor(){
        return color;
    }
}
