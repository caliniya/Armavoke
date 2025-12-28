package caliniya.armavoke.game.type;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.type.CType;

public class WeaponType {
    
    public String name;
    public TextureRegion region;
    
    public float w = 10f, h = 10f;
    public float range = 1000f;
    public float rotateSpeed = 5f;
    public float reload = 60f;
    public float x = 0f, y = 0f;

    public WeaponType(String name) {
        this.name = name;
    }

    /**
     * 在 UnitType.load() 中被调用
     * @param parentUnit 该武器所属的单位类型
     */
    public void load(UnitType parentUnit) {
      
        // 拼接名称： "unitname-weaponname"
        String textureName = parentUnit.name + "-" + this.name;
        
        // 加载纹理
        region = Core.atlas.find(textureName , "white");
    }
}