# 部署说明

本项目有两个可独立部署的产物，依赖完全不同：

| 产物 | 形态 | 运行需要 |
| --- | --- | --- |
| `index.html` | 单个 HTML 文件（76KB） | 只要一个浏览器。无 CDN、无外部字体、无后端 |
| `公考错题本-v3.0.0.apk` | 安卓安装包 | Android 8.0+ 手机 |
| `android/` | 原生工程源码 | JDK 17 + Android SDK 34 + Gradle 8.7 |

---

## 一、网页版

### 前置依赖

**没有。** 不需要 Node、不需要 Python、不需要数据库。分类表已经内嵌在 `index.html` 里（`<script id="catalog-data">`），运行时不会去读 `catalog.json`。

唯一的要求是浏览器允许 `localStorage`。Chrome / Edge / Safari / 微信内置浏览器都支持。

### 四种部署方式

#### A. 本机双击打开（`file://`）

把 `index.html` 放在任意目录，双击。

- 适用：只在这台电脑上用，图省事
- 坑：`localStorage` 按**文件路径**隔离。换目录、改文件名，数据就"不见了"（其实还在旧路径下）。部分浏览器（如 Firefox 的某些配置）会直接禁用 `file://` 的本地存储

#### B. 局域网 HTTP 服务

```bash
cd 错题记录
python -m http.server 8080 --bind 0.0.0.0
```

手机连同一个 WiFi，浏览器打开 `http://<电脑内网IP>:8080`。

- 适用：手机想用大屏之外的方式记录，不想上传公网
- 优点：数据按域名维度隔离，比 `file://` 稳；可「添加到主屏幕」
- 限制：电脑得开着、同一个网络；出门就用不了
- 查内网 IP：`ipconfig` 里的 IPv4 地址

#### C. 静态托管（推荐用于长期自用）

把 `index.html` 上传到任意静态托管，得到一个固定网址：

| 平台 | 操作 | 备注 |
| --- | --- | --- |
| GitHub Pages | 仓库 Settings → Pages → Source 选 `main` / 根目录 | 免费；国内访问可能需要代理 |
| Vercel / Netlify | 拖入文件夹或连仓库 | 免费额度够用 |
| 腾讯云 COS / 阿里云 OSS | 开「静态网站」功能，上传 index.html，设默认首页 | 国内速度快，按量计费（几毛钱/月） |
| 自己的服务器 | 放到 Nginx 的 `root` 目录即可 | 需要一个域名或公网 IP |

- 适用：手机随时能用、换设备不用传文件
- 注意：托管地址一旦变了就是新的「源」，旧数据不会跟过去（见第五节）

#### D. 传文件到手机本地

把 `index.html` 发到手机，用文件管理器选择「用浏览器打开」。

- 适用：完全离线、只想在手机上看
- 缺点：`file://` 的数据隔离问题同 A，且每次更新都要重新传

### 方式对比

| 方式 | 成本 | 手机能否用 | 多设备 | 数据稳定性 | 适合谁 |
| --- | --- | --- | --- | --- | --- |
| A 双击 | 0 | 需传文件 | 否 | 换路径即丢 | 只在电脑上记录 |
| B 局域网 | 0 | 同 WiFi 可用 | 否 | 较好 | 家里/宿舍固定场景 |
| C 静态托管 | 0～低 | 随时 | 手动导出导入 | 最好 | **日常主力** |
| D 手机本地 | 0 | 仅本机 | 否 | 一般 | 纯离线 |

**推荐：C。** 理由是入口稳定（一个固定网址，加到主屏幕就是半个 App），并且换手机、换电脑时只要导出/导入备份就能搬走。如果预算为零且不想碰 GitHub，用**腾讯云 COS 静态网站**最省心，国内访问也快。

### 验证部署成功

按顺序过一遍，每步都是可观察的结果：

1. 打开页面 → 顶部是「公考错题本」，下方深色卡片显示「累计错误记录」，再往下是 5 个模块
2. 「言语理解」卡片右侧应显示 `题库 6762 题 · 3 个题型`，判断推理 `7728 题 · 5 个题型`，资料分析 `3521 题 · 6 个题型` ← 这行能对上说明内嵌分类表完好
3. 点「言语理解」→ 出现 3 个题型卡（片段阅读 / 逻辑填空 / 语句表达）
4. 点「片段阅读」→ 顶部有蓝色「子类题量」条，子类按钮分两行铺开（中心理解 1600、细节判断 918…）
5. 点底部「新增知识点」→ 弹窗里子类带题量、名称下方出现推荐词（关联词-对策、主题词…）、错因是片段阅读专属的（没抓中心句、以偏概全…）
6. 保存 → 清单出现该知识点；点左侧圆圈 → 底部提示「已记录…撤销」，数字 +1
7. **刷新页面** → 数字还在 ← 这一条验证 `localStorage` 真的可用
8. 右上角「ⓘ」→ 导出备份 → 浏览器下载 `cuotiben-backup-YYYYMMDD.json`
9. 用另一台设备/另一个浏览器打开同一地址 → 导入备份 → 数据一致

第 7 步最关键：如果刷新后数据没了，说明当前环境的 `localStorage` 被禁用（常见于隐私模式、部分 App 内置浏览器）。

---

## 二、安卓版

### 前置依赖

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 | 编译 Kotlin |
| Android SDK | platform 34 + build-tools 34.0.0 | 编译目标 |
| Gradle | 8.7 | 仓库带 wrapper，但本机 `C:\avb` 有现成缓存 |
| 网络 | 首次编译需拉依赖 | 之后可离线 |

本机已有便携工具链在 `C:\avb`（jdk / sdk / gradle / gradle-home），无需另装。

### 构建

**方式一：双击 `android\rebuild.bat`**（已封装环境变量，路径写死在脚本里）

**方式二：命令行**

```bash
export JAVA_HOME="C:/avb/jdk"
export ANDROID_HOME="C:/avb/sdk"
export GRADLE_USER_HOME="C:/avb/gradle-home"
cd android && "C:/avb/gradle/bin/gradle" assembleDebug
```

**方式三：Android Studio** → Open → 选 `android` 目录 → Sync → Run

产物：`android/app/build/outputs/apk/debug/app-debug.apk`

### 需要改的配置

| 文件 | 配置项 | 什么时候改 |
| --- | --- | --- |
| `android/local.properties` | `sdk.dir` | 换电脑或 SDK 路径变了 |
| `android/rebuild.bat` | `JAVA_HOME` / `ANDROID_HOME` / `GRADLE_BIN` | 不用 `C:\avb` 那天 |
| `android/gradle.properties` | `android.overridePathCheck=true` | 工程不在中文路径下时可删 |
| `android/app/build.gradle.kts` | `versionCode` / `versionName` | 每次发版递增 |

### 验证部署成功

1. 安装后首次打开 → 主界面 5 个模块，言语理解显示「题库 6762 题 · 3 个题型」
2. 进「言语理解」→ 必须是 **3 个题型**（片段阅读 / 逻辑填空 / 语句表达）。如果只看到 2 个，说明 `assets/catalog.json` 没打进包
3. 从旧版升级的：原知识点应落在对应题型下；对不上题库分类的会出现在题型列表底部的「未归类」卡片里
4. 新增知识点 → 子类带题量、错因是该题型专属的
5. 勾选 → 提示条可撤销；进详情 → 每条记录右侧「⋯」可补题号与备注
6. 杀进程重开 → 数据还在

---

## 三、分类表更新（改了题库之后）

分类不是手写的，改题库或调阈值后重新生成：

```bash
python tools/gen_catalog.py       # 扫题库 → catalog.json
python tools/inject_catalog.py    # 注入 index.html
cp catalog.json android/app/src/main/assets/catalog.json
```

三个产物必须一致，否则网页版和 APK 的分类会对不上。

`tools/gen_catalog.py` 里需要确认的路径：

```python
BANK_DIR = os.path.join(os.path.expanduser("~"), "Documents", "错题复盘", "app", "dist", "data")
```

题库不在这个位置就要改。

---

## 四、数据存放在哪

| 端 | 位置 | 清除方式 |
| --- | --- | --- |
| 网页版 | 浏览器的 `localStorage`，键名 `gk_cuoti_v3` | 清站点数据 / 无痕窗口关闭即消失 |
| 安卓版 | App 私有目录的 `cuotiben.db`（SQLite） | 卸载 App |

两边**数据不互通**，要搬家只能走「导出备份 → 导入备份」。

---

## 五、换部署地址时最重要的一件事

`localStorage` 是按**源**隔离的：`file:///C:/a/index.html`、`http://192.168.1.5:8080`、`https://xxx.pages.dev` 是三个完全独立的空间。

所以**换部署方式之前，一定要先在旧地址导出备份**，到新地址再导入。否则会以为"数据丢了"。

导出/导入入口：页面右上角「ⓘ」→ 导出备份 / 导入备份。

导入是**合并**语义，不是覆盖：同名同分类的知识点会合并（次数取大、错因取新），记录按时间戳去重，所以重复导入不会把次数翻倍。
