# Git 与 GitHub 使用指南

> 本文档记录了如何将本地项目托管到 GitHub 的完整流程。

---

## 一、基础概念

### 1.1 什么是 Git？

Git 是一个**分布式版本控制系统**，可以记录文件的每次修改历史，让你随时回退到任意版本。

**解决的问题**：
```
论文_v1.doc
论文_v2_修改.doc
论文_v3_导师意见.doc
论文_v4_最终版.doc
论文_v5_打死不改版.doc
```
→ 使用 Git 后，只需要一个文件，所有历史版本都有记录。

### 1.2 什么是 GitHub？

GitHub 是基于 Git 的**代码托管平台**，提供：
- 云端备份代码
- 团队协作开发
- 开源社区交流
- 展示技术能力

### 1.3 Git 的三个区域

```
┌─────────────┐    git add    ┌─────────────┐   git commit   ┌─────────────┐
│  工作区     │  ──────────→  │   暂存区    │  ────────────→ │  本地仓库   │
│ (你改的代码) │              │ (准备提交的) │               │ (提交历史)  │
└─────────────┘              └─────────────┘               └─────────────┘
                                                                  │
                                                                 │ git push
                                                                 ▼
                                                          ┌─────────────┐
                                                          │  GitHub     │
                                                          │ (远程仓库)   │
                                                          └─────────────┘
```

| 区域 | 说明 |
|------|------|
| 工作区 | 你实际编辑代码的地方 |
| 暂存区 | 用 `git add` 添加后，准备提交的内容 |
| 本地仓库 | 用 `git commit` 后，保存在本地的版本历史 |
| 远程仓库 | GitHub 上的仓库，用 `git push` 推送 |

---

## 二、完整提交流程（首次托管项目）

### 2.1 环境准备

#### 检查 Git 是否安装
```bash
git --version
```

#### 配置用户信息（首次使用需要配置）
```bash
git config --global user.name "你的用户名"
git config --global user.email "你的邮箱"
```

#### 生成 SSH 密钥（用于免密推送）
```bash
ssh-keygen -t rsa -C "你的邮箱"
```
一路回车，生成的公钥在 `~/.ssh/id_rsa.pub`。

将公钥添加到 GitHub：
1. GitHub → Settings → SSH and GPG keys
2. New SSH key → 粘贴公钥 → Add SSH key

---

### 2.2 初始化仓库

在项目根目录执行：

```bash
git init
```

**作用**：在当前目录创建 `.git` 文件夹，将其变成 Git 仓库。

---

### 2.3 创建 .gitignore 文件

`.gitignore` 文件告诉 Git 哪些文件不需要跟踪。

**示例内容**：
```gitignore
# 依赖目录
node_modules/
target/

# 构建输出
dist/
build/

# IDE 配置
.idea/
.vscode/
*.iml

# 系统文件
.DS_Store
Thumbs.db

# 日志
*.log

# 环境配置（可能含敏感信息）
.env
.env.local
```

**作用**：避免提交不必要的文件（如依赖包、编译产物、IDE配置等）。

---

### 2.4 添加文件到暂存区

```bash
# 添加所有文件
git add .

# 或添加指定文件
git add 文件名
```

**作用**：将工作区的修改添加到暂存区，准备提交。

---

### 2.5 提交到本地仓库

```bash
git commit -m "提交说明"
```

**示例**：
```bash
git commit -m "Initial commit: 超市销售预测系统毕业设计项目"
```

**作用**：将暂存区的内容提交到本地仓库，形成一个版本记录。

**提交说明规范**：
- `feat: 添加用户登录功能`
- `fix: 修复分页显示问题`
- `docs: 更新 README 文档`
- `refactor: 重构订单模块代码`
- `style: 代码格式调整`

---

### 2.6 在 GitHub 创建远程仓库

1. 访问 https://github.com
2. 点击右上角 **+** → **New repository**
3. 填写仓库名称（如 `GraduationProject`）
4. 选择 Public 或 Private
5. **不要勾选** "Initialize this repository with a README"
6. 点击 **Create repository**

创建后会得到仓库 URL，如：
```
https://github.com/用户名/仓库名.git
```

---

### 2.7 连接远程仓库

```bash
# 添加远程仓库
git remote add origin git@github.com:用户名/仓库名.git

# 查看远程仓库
git remote -v
```

**作用**：将本地仓库与 GitHub 上的远程仓库关联。

---

### 2.8 推送到 GitHub

```bash
# 设置主分支名称为 main
git branch -M main

# 推送到远程仓库
git push -u origin main
```

**作用**：将本地代码推送到 GitHub。

`-u` 参数表示设置上游分支，之后可以直接用 `git push` 推送。

---

## 三、日常开发流程

每次完成一个功能或修改后：

```bash
# 1. 查看修改状态
git status

# 2. 添加修改到暂存区
git add .

# 3. 提交到本地仓库
git commit -m "描述你的修改"

# 4. 推送到 GitHub
git push
```

### 3.1 常用命令速查

| 命令 | 作用 |
|------|------|
| `git status` | 查看当前状态（哪些文件被修改） |
| `git add .` | 添加所有修改到暂存区 |
| `git add 文件名` | 添加指定文件到暂存区 |
| `git commit -m "说明"` | 提交暂存区内容 |
| `git push` | 推送到远程仓库 |
| `git pull` | 拉取远程仓库最新代码 |
| `git log` | 查看提交历史 |
| `git diff` | 查看未暂存的修改 |
| `git diff --staged` | 查看已暂存的修改 |

---

## 四、分支操作

### 4.1 什么是分支？

分支可以理解为"平行宇宙"，让你在不影响主分支的情况下独立开发新功能。

```
main ──────●─────●─────●─────●
                   \
feature-login      ●─────●─────●
```

### 4.2 分支常用命令

```bash
# 查看分支
git branch

# 创建新分支
git branch 分支名

# 切换分支
git checkout 分支名

# 创建并切换分支
git checkout -b 分支名

# 合并分支（先切回主分支）
git checkout main
git merge 分支名

# 删除分支
git branch -d 分支名
```

### 4.3 分支工作流

```bash
# 1. 创建功能分支
git checkout -b feature-login

# 2. 在分支上开发、提交
git add .
git commit -m "feat: 完成登录页面"

# 3. 推送分支到远程
git push origin feature-login

# 4. 在 GitHub 创建 Pull Request（可选）

# 5. 合并到主分支
git checkout main
git merge feature-login
git push

# 6. 删除功能分支
git branch -d feature-login
```

---

## 五、回退操作

### 5.1 撤销工作区修改（未 add）

```bash
git checkout -- 文件名
```

### 5.2 撤销暂存区修改（已 add，未 commit）

```bash
git reset HEAD 文件名
```

### 5.3 回退到历史版本（已 commit）

```bash
# 查看提交历史，找到要回退的版本号
git log

# 回退到指定版本（保留工作区修改）
git reset --soft 版本号

# 回退到指定版本（丢弃所有修改）
git reset --hard 版本号
```

---

## 六、常见问题

### Q1: push 时提示权限错误？

确保 SSH 密钥已添加到 GitHub：
```bash
ssh -T git@github.com
```
成功会显示：`Hi 用户名! You've successfully authenticated...`

### Q2: 如何查看某个文件的修改历史？

```bash
git log --follow 文件名
git show 版本号:文件名  # 查看某版本的文件内容
```

### Q3: 如何同步其他人推送的代码？

```bash
git pull origin main
```

### Q4: 推送时出现冲突怎么办？

1. 先拉取最新代码：`git pull`
2. 手动解决冲突（冲突文件会有标记）
3. 重新提交：`git add . && git commit -m "解决冲突"`
4. 推送：`git push`

---

## 七、最佳实践

1. **频繁提交**：每完成一个小功能就提交，不要攒太多
2. **清晰的提交信息**：让人一看就知道改了什么
3. **推送前先拉取**：`git pull` → 解决冲突 → `git push`
4. **使用分支**：新功能在分支上开发，完成后再合并
5. **不要提交敏感信息**：密码、密钥等放入 `.gitignore`

---

## 八、本次项目提交流程记录

以下是本次将 `GraduationProject` 项目托管到 GitHub 的完整操作：

```bash
# 1. 初始化 Git 仓库
git init

# 2. 添加所有文件到暂存区
git add .

# 3. 提交到本地仓库
git commit -m "Initial commit: 超市销售预测系统毕业设计项目"

# 4. 添加远程仓库
git remote add origin git@github.com:FengLoveLife/GraduationProject.git

# 5. 设置主分支名称
git branch -M main

# 6. 推送到 GitHub
git push -u origin main
```

---

*文档创建时间：2026-03-12*