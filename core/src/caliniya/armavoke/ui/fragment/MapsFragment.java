package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import arc.util.Log;
import caliniya.armavoke.core.InitGame;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.map.Maps;
import caliniya.armavoke.ui.Button;
import caliniya.armavoke.ui.Styles;

public class MapsFragment extends WinFragment {
    
    private Table mapList;
    private ScrollPane scrollPane;
    private TextField searchField;
    private String searchText = "";
    
    @Override
    public void main(Table t) {
        t.table(Styles.buttondef.up, body -> {
            // 标题栏和搜索框
            body.table(header -> {
                header.add("Maps").growX().left().padLeft(8);
                
                // 搜索框
                searchField = header.field(null, text -> {
                    searchText = text;
                    reloadMaps();
                }).width(180).padRight(4).get();
                searchField.setMessageText("Search...");
                
                // 刷新按钮 - 使用 Button 构造函数
                Button refreshButton = new Button("Refresh", () -> {
                    Maps.load();
                    reloadMaps();
                });
                header.add(refreshButton).padRight(8);
                
            }).growX().height(42).padBottom(8);
            
            body.row();
            
            // 排序选项
            body.table(sortOptions -> {
                sortOptions.add("[gray]Sort:[] ").left();
                
                // 名称排序按钮
                Button nameSort = new Button("[white]Name[]", () -> {
                    // 默认已经按名称排序
                    reloadMaps();
                });
                sortOptions.add(nameSort).padLeft(8);
                
                // 类型排序按钮
                Button typeSort = new Button("[white]Type[]", () -> {
                    Maps.maps.sort((a, b) -> {
                        int type = Boolean.compare(a.custom, b.custom);
                        if (type != 0) return type;
                        return a.plainName().compareTo(b.plainName());
                    });
                    reloadMaps();
                });
                sortOptions.add(typeSort).padLeft(4);
                
                sortOptions.add().growX();
            }).growX().padBottom(4).padLeft(4);
            
            body.row();
            
            // 地图列表容器
            mapList = new Table();
            scrollPane = new ScrollPane(mapList);
            scrollPane.setFadeScrollBars(false);
            scrollPane.setScrollingDisabled(true, false);
            
            body.add(scrollPane).grow().pad(4);
        }).grow();
        
        // 加载地图列表
        reloadMaps();
    }
    
    /** 重新加载地图列表，应用搜索过滤 */
    public void reloadMaps() {
        mapList.clear();
        
        int count = 0;
        
        // 遍历所有地图并创建列表项
        for (Map map : Maps.maps) {
            // 应用搜索过滤
            if (!searchText.isEmpty() && 
                !map.name().toLowerCase().contains(searchText.toLowerCase()) &&
                !map.author().toLowerCase().contains(searchText.toLowerCase()) &&
                !map.description().toLowerCase().contains(searchText.toLowerCase())) {
                continue;
            }
            
            createMapItem(map);
            mapList.row();
            count++;
        }
        
        // 如果没有匹配的地图，显示提示信息
        if (count == 0) {
            if (Maps.maps.isEmpty()) {
                mapList.add("[lightgray]No maps found. Click Refresh to scan for maps.[]").pad(20).center();
            } else {
                mapList.add("[lightgray]No maps matching your search.[]").pad(20).center();
            }
        }
    }
    
    /** 创建单个地图项 */
    private void createMapItem(Map map) {
        // 地图项容器 - 使用 Button 包装整个地图项
        Table itemContent = new Table();
        
        // 预览图区域 (左侧)
        Table previewTable = new Table();
        if (map.safeTexture() != null) {
            Image preview = new Image(map.safeTexture());
            preview.setScaling(Scaling.fit);
            previewTable.add(preview).size(80, 60);
        } else {
            previewTable.add("[gray]No Preview[]").size(80, 60).center();
        }
        itemContent.add(previewTable).size(84, 64).pad(4);
        
        // 信息区域 (右侧)
        Table infoTable = new Table();
        
        // 第一行：名称 + 类型标签
        Table nameRow = new Table();
        Label nameLabel = new Label(map.name());
        nameLabel.setWrap(true);
        nameRow.add(nameLabel).growX().left();
        
        // 类型标签
        String typeText = map.custom ? "Custom" : "Built-in";
        String typeColor = map.custom ? "#98C87A" : "#7BA4C8";
        nameRow.add("[" + typeColor + "]<" + typeText + ">[]").padLeft(8);
        
        if (map.space) {
            nameRow.add("[#C87AA4]<Space>[]").padLeft(4);
        }
        
        infoTable.add(nameRow).growX().left().padBottom(4);
        
        infoTable.row();
        
        // 第二行：作者
        if (!map.author().equals("Unknown")) {
            infoTable.add("[gray]By " + map.author() + "[]").left().padBottom(2);
            infoTable.row();
        }
        
        // 第三行：尺寸
        if (map.width > 0 && map.height > 0) {
            infoTable.add("[lightgray]Size: " + map.width + "x" + map.height + "[]")
                .left().padBottom(2);
            infoTable.row();
        }
        
        // 第四行：描述
        if (!map.description().isEmpty()) {
            Label descLabel = new Label("[gray]" + map.description() + "[]");
            descLabel.setWrap(true);
            infoTable.add(descLabel).growX().left().padTop(2);
        }
        
        itemContent.add(infoTable).growX().pad(8);
        
        // 使用 Button 包装，Button 构造函数为 Button(String text, Runnable listener)
        Button mapButton = new Button("", () -> {
            onMapSelected(map);
        });
        
        // 将内容添加到按钮中
        mapButton.clearChildren();
        mapButton.add(itemContent).grow();
        
        // 添加到列表
        mapList.add(mapButton).growX().pad(4);
    }
    
    /** 当选择地图时的回调 */
    private void onMapSelected(Map map) {
        Log.info("Selected map: " + map.name());
        // 在这里处理地图选择逻辑
    }
}