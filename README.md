# 属性骰子

这是一个 Minecraft Java 1.21.11 Fabric 模组，玩家可以使用稀有材料制作属性骰子来增强属性！投出高点数获得属性（伤害 / 护甲 / 最大生命值）；投出低点数失去属性。小心投出1点——会被闪电击中！

> 提示：过度赌博可能会伤害到你，所以不要过度使用！

## 特性

- **合成配方**（3x3）：四角为金块，中心为下界合金块，其余四个格子为钻石。
- **右键点击**骰子物品在你面前投掷一个旋转的骰子实体。约3秒后停止并显示投出的点数。
- **4 / 5 / 6（高点）**：获得随机属性（攻击伤害 / 护甲 / 最大生命值），数值在 `[gainMin, gainMax]` 范围内（默认 `[1, 10]`）。
- **1 / 2 / 3（低点）**：失去随机属性，数值在 `[lossMin, lossMax]` 范围内（默认 `[1, 5]`）。
- **投出1点**：额外造成 `lightningDamage` 闪电伤害（默认10）并召唤闪电击中玩家。
- 每个面（1–6）出现的概率相同。
- 聊天消息显示投出的点数：1/2/3 为红色，4/5/6 为绿色。

以上所有参数都可以通过 `config/attribute_dice.json` 配置（首次运行时自动创建）。

## 构建

要求：**JDK 21** 和网络连接（Gradle 将下载 Minecraft 映射文件）。

```bat
gradlew.bat build
```

输出 jar：`build/libs/attribute-dice-1.0.0.jar` — 将其放入 Fabric 的 `mods/` 文件夹中，并与 Fabric API 一起使用。

## 纹理放置

模组默认**不包含**任何纹理——请将您自己的 PNG 图片放入以下 `src/main/resources/assets/attribute_dice/textures/` 目录下：

| 用途         | 路径                                                          | 推荐大小 |
|-------------|---------------------------------------------------------------|----------|
| 物品图标     | `item/attribute_dice.png`                                      | 16x16    |
| 实体纹理     | `entity/dice.png`（一个纹理用于立方体的所有6个面）                | 16x16    |

目录结构示例：

```
src/main/resources/assets/attribute_dice/
├── lang/
│   ├── en_us.json
│   └── zh_cn.json
├── models/
│   └── item/
│       └── attribute_dice.json
└── textures/
    ├── item/
    │   └── attribute_dice.png   <-- 你的物品图标放这里
    └── entity/
        └── dice.png             <-- 你的立方体面纹理放这里
```

放入纹理后，重新运行 `gradlew.bat build` 将其打包到 jar 中。
