package caliniya.armavoke.base.game;

import arc.util.Nullable;
import arc.util.pooling.Pool.Poolable;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.data.TeamData;

public abstract class Entity {
  
  //瓦片坐标
  public int tx , ty ;
  
  //像素坐标
  public float x, y;
  
  public float health,maxHealth;
  
  public int id;
  public TeamTypes team;
  public TeamData teamData;
  
  public void hit(){
    hit(null);
  }
  public abstract void hit(@Nullable Entity origin);

  public abstract void heal();
}
