# 高德离线自定义样式

免费版 GeoHUB 创建并发布样式后，在“使用方法 → Android → 下载离线文件”中下载与项目地图 SDK 版本匹配的压缩包。

解压后把以下文件放在本目录：

- `style.data`（必需）
- `style_extra.data`（可选）
- `textures.zip`（可选；需要对应纹理权限）

应用会优先读取这里的离线样式；若没有 `style.data`，才会尝试 `local.properties` 中的 `AMAP_STYLE_ID`，最后回退到高德标准样式。

