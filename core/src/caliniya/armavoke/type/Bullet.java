package caliniya.armavoke.type;

import arc.util.pooling.Pool.Poolable;
import arc.util.pooling.Pools;
import caliniya.armavoke.base.type.TeamTypes;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.type.type.BulletType;

public class Bullet implements Poolable {
    public BulletType type;
    public Unit owner;      // 发射者 (防止自伤)
    public TeamTypes team;  // 所属团队 (用于敌我识别)
    
    public float x, y;
    public float velX, velY;
    public float rotation;
    public float time = 0f; // 已存活时间
    
    // public Bullet next; 

    protected Bullet() {}

    /** 工厂方法：创建并初始化子弹 */
    public static Bullet create(BulletType type, Unit owner, float x, float y, float angle) {
        Bullet b = Pools.obtain(Bullet.class, Bullet::new);
        b.init(type, owner, x, y, angle);
        return b;
    }

    public void init(BulletType type, Unit owner, float x, float y, float angle) {
        this.type = type;
        this.owner = owner;
        this.team = (owner != null) ? owner.team : TeamTypes.Abort;
        
        this.x = x;
        this.y = y;
        this.rotation = angle;
        this.time = 0f;
        
        // 计算速度向量
        this.velX = arc.math.Mathf.cosDeg(angle) * type.speed;
        this.velY = arc.math.Mathf.sinDeg(angle) * type.speed;
        
        // 加入全局子弹列表
        WorldData.bullets.add(this);
    }

    @Override
    public void reset() {
        type = null;
        owner = null;
        team = null;
        x = 0; y = 0;
        velX = 0; velY = 0;
        time = 0;
    }

    /** 移除子弹 (回收到池) */
    public void remove() {
        WorldData.bullets.remove(this);
        Pools.free(this);
    }
}