# MP3 Launcher

复古 MP3 播放器风格的 Android 启动器。它把日常桌面、音乐控制、实体音量键快捷操作和复古拟物界面放在同一个主屏里，目标是像一台可用的随身播放器一样使用手机桌面。

![fig1](/tempshow/fig1.jpg)
![fig2](/tempshow/fig2.jpg)

## 功能总览

### 音乐主屏

- 显示当前媒体标题、作者、播放/暂停状态、播放进度、总时长、音量和电量。
- 通过通知访问读取系统媒体通知和媒体会话，优先使用 `MediaController` 控制播放器。
- 没有可用媒体会话时，会回退为系统媒体按键事件。
- 专辑封面会做像素化和轻微褪色处理，贴近复古 LCD 屏幕效果。
- 播放/暂停、上一首、下一首、快退、快进、音量加减都有模拟实体按键。
- 长按播放/暂停键会打开设置里选择的默认音乐 App。
- 可选“模拟屏幕横滑切歌”：在 LCD 屏幕左滑下一首，右滑上一首，并带短促滑动反馈动画。

### 复古界面

- 主体界面由应用栏、播放器机身、像素 LCD、按键区和底部装饰区组成。
- 支持竖屏和横屏两套布局：
  - 竖屏保留上方应用栏和上下播放器结构。
  - 横屏应用栏变为左侧纵向滚动，播放器屏幕和按键区左右排列。
- 支持多套主题皮肤：黑绿 LCD、银色 Discman、透明 iPod、Game Boy、Walkman、红色磁带机、冰蓝 MiniDisc、薄荷奶油、午夜紫、日落收音机。
- 开机时可显示像素磁带播放机 LOADING 动画。
- 音频输出指示灯显示当前输出路线：
  - 红色：扬声器
  - 蓝色：蓝牙
  - 绿色：有线耳机
- 两个拟物拨杆位于主界面底部：
  - `<>` 拨杆：开启/关闭实体音量键切歌模式。
  - `HOLD` 拨杆：开启/关闭极简模式。
- 极简模式会隐藏应用栏和搜索入口，保留播放器主体。
- 横屏和竖屏都有机身装饰文字，横屏装饰文字前带有分隔竖线。

### Launcher 能力

- 应用栏展示可启动应用、搜索入口和 MP3 Launcher 设置入口。
- 设置入口作为普通图标参与排序，不再默认置顶。
- 默认导航标签为 `ALL` 和 `RECENT`。
- 最近使用列表会记录普通应用和设置入口，最多保留 24 个。
- 支持搜索应用。
- 支持置顶应用，置顶应用在普通排序中排在前面。
- 支持隐藏应用，隐藏后不会出现在应用栏。
- 长按应用图标弹出操作菜单：
  - 置顶/取消置顶
  - 隐藏
  - 应用详情
- 长按设置图标会直接打开设置页。
- 支持自定义应用标签分组：
  - 创建自定义分组
  - 重命名分组
  - 删除分组
  - 给应用分配或移出分组
  - 一键清除手动分组覆盖，回到自动分组
- 应用栏支持滑动切换标签，并显示短暂的字母/标签提示。

### 实体音量键

启动器前台时，实体音量键有三种模式：

- `Track + play`
  - 音量上短按：上一首
  - 音量下短按：下一首
  - 长按任意音量键：播放/暂停
- `Volume + play`
  - 音量上短按：增加媒体音量
  - 音量下短按：降低媒体音量
  - 长按任意音量键：播放/暂停
- `System default`
  - 交还系统默认音量键行为

锁屏和屏幕熄灭时，`Track + play` 模式会启用额外链路：

- 锁屏亮屏时通过无障碍服务捕获实体音量键，只处理音量上/下按键。
- 屏幕熄灭时通过本应用的 `MediaSession` 和 `VolumeProvider` 接收相对音量调整事件。
- 音量上短按映射为上一首，音量下短按映射为下一首。
- 锁屏亮屏时长按音量键会触发播放/暂停。
- 关闭主界面 `<>` 拨杆或在设置中切到其他音量键模式后，会停止屏幕熄灭音量键切歌会话。

## 设置页

设置页支持跟随系统、英文和简体中文。主要设置包括：

- 通知访问权限入口。
- 无障碍设置入口，并说明用途：仅用于锁屏时捕获实体音量键，不读取屏幕内容。
- 默认桌面设置入口。
- 未播放时默认音乐 App 选择。
- 实体音量键模式选择。
- 极简模式开关。
- 开机动画开关。
- 模拟屏幕横滑切歌开关。
- 主题皮肤切换。
- 置顶应用管理。
- 隐藏应用管理。
- 选择要隐藏的应用。
- 自定义应用标签分组管理。
- 清除所有手动分组覆盖。

## 权限说明

### 通知访问

用于读取当前媒体通知、获取媒体会话、同步歌曲信息和发送播放控制。没有通知访问时，界面仍可打开，但歌曲信息和部分媒体控制可能无法稳定同步。

入口：

```text
设置 -> 通知访问
```

### 无障碍服务

用于在锁屏亮屏状态捕获实体音量键，让 MP3 Launcher 可以把音量键映射为上一首/下一首/播放暂停。服务声明 `canRetrieveWindowContent=false`，不会读取屏幕内容。

入口：

```text
设置 -> 无障碍 -> MP3 Launcher
```

### 默认桌面

用于让 MP3 Launcher 成为系统 Home 应用。首次进入时会提示设置默认桌面，设置页中也提供入口。

入口：

```text
设置 -> 默认应用 -> 桌面应用
```

## 当前限制

- 锁屏和息屏音量键切歌依赖系统媒体会话、播放器支持情况和设备厂商的锁屏策略；不同 Android 版本或厂商系统可能有差异。
- 若当前没有可控制的媒体会话，应用会尝试回退发送系统媒体按键，但并非所有播放器都会响应。
- 无障碍服务只用于锁屏实体音量键，不提供屏幕读取、自动点击或界面分析能力。
- 本项目当前主导航只默认显示 `ALL`、`RECENT` 和用户创建的自定义标签。

## 构建与安装

项目使用 Android Gradle Plugin 和 Kotlin。

编译 Debug 包：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew :app:assembleDebug
```

运行单元测试：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew :app:testDebugUnitTest
```

同时构建和测试：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

安装到当前连接设备：

```bash
/Users/chaohsuan/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

指定设备安装示例：

```bash
/Users/chaohsuan/Library/Android/sdk/platform-tools/adb -s R5CTA1XVEFZ install -r app/build/outputs/apk/debug/app-debug.apk
```

清理项目内构建缓存后重新构建：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew --stop
rm -rf .gradle build app/build
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## 开发备注

- 媒体控制逻辑集中在 `MediaCommandDispatcher`。
- 通知监听由 `MediaNotificationListenerService` 负责。
- 锁屏亮屏实体音量键由 `LockScreenVolumeKeyService` 处理。
- 屏幕熄灭实体音量键由 `ScreenOffVolumeKeySession` 处理。
- 主界面状态和应用列表由 `MainViewModel` 维护。
- 设置项存储在 `LauncherPreferences`。
- 新界面改动应继续保持“除应用栏外，主体是复古拟物播放器”的方向。
