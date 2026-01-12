import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import deque
from rich.console import Console
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
from rich.live import Live
from rich.text import Text
from rich.traceback import install

install()
console = Console()

# ================= 配置区 (RIA-J) =================
TEMPLATE_FILE = ".zenodo.template.json"
OUTPUT_FILE = ".zenodo.json"
BUILD_COMMAND = "mvnd clean package" # 如果没有 mvnd，请改为 "mvn clean package"
LOG_HEIGHT = 12
# ==================================================

def run_process_with_live_log(command, live, log_lines, generate_panel_func, allow_failure=False):
    """运行单个命令，并将输出实时喂给 Live 面板"""
    log_lines.append(f"[dim]⚡ 执行: {command}[/]")
    live.update(generate_panel_func())

    process = subprocess.Popen(
        command, 
        shell=True, 
        stdout=subprocess.PIPE, 
        stderr=subprocess.STDOUT, 
        text=True,
        encoding='utf-8', 
        errors='replace'
    )

    while True:
        line = process.stdout.readline()
        if not line and process.poll() is not None:
            break
        if line:
            clean_line = line.strip()
            if clean_line:
                log_lines.append(clean_line)
                live.update(generate_panel_func())

    if process.returncode != 0:
        if allow_failure:
            log_lines.append(f"[yellow]⚠️  该步骤失败但被忽略 (允许失败)[/]")
            live.update(generate_panel_func())
            return True
        else:
            return False
    return True

def run_sequence_in_window(steps, title, final_success_msg):
    """在滚动窗口中运行一系列命令"""
    log_lines = deque(maxlen=LOG_HEIGHT)
    
    def generate_panel():
        log_content = Text.from_markup("\n".join(log_lines))
        return Panel(
            log_content,
            title=f"[bold blue]⏳ {title}[/]",
            border_style="blue",
            height=LOG_HEIGHT + 2,
            padding=(0, 1)
        )

    with Live(generate_panel(), refresh_per_second=10, console=console) as live:
        for cmd, allow_fail in steps:
            success = run_process_with_live_log(cmd, live, log_lines, generate_panel, allow_fail)
            if not success:
                console.print(Panel(f"[bold red]❌ 执行失败！[/]\n命令: {cmd}\n请检查上方日志。", style="red"))
                sys.exit(1)
    
    console.print(f"[bold green]✅ {final_success_msg}[/]")

def get_pom_version():
    pom_file = "pom.xml"
    if not os.path.exists(pom_file):
        console.print(f"[bold red]❌ 错误: 找不到 {pom_file}[/]")
        sys.exit(1)
    try:
        tree = ET.parse(pom_file)
        root = tree.getroot()
        # 查找 project 下的 version 标签
        for child in root:
            if 'version' in child.tag:
                return child.text.strip()
        console.print("[bold red]❌ 错误: pom.xml 中无 <version> 标签[/]")
        sys.exit(1)
    except Exception as e:
        console.print(f"[bold red]❌ 解析 pom.xml 失败: {e}[/]")
        sys.exit(1)

def ask_for_version(detected_version):
    console.print(Panel.fit(
        f"🔍 检测到 pom.xml 版本: [bold cyan]{detected_version}[/]",
        title="RIA-J 版本检测", border_style="blue"
    ))
    return Prompt.ask("📝 请确认发布版本号", default=detected_version)

def build_project():
    console.rule("[bold green]🔨 第一步：构建项目 (RIA-J)[/]")
    steps = [(BUILD_COMMAND, False)]
    run_sequence_in_window(steps, "正在执行 Maven 构建...", "构建完成")

def generate_zenodo_json(version):
    if not os.path.exists(TEMPLATE_FILE):
        console.print(f"[bold red]❌ 找不到模板: {TEMPLATE_FILE}[/]")
        sys.exit(1)
    with open(TEMPLATE_FILE, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = content.replace("{{VERSION}}", version)
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write(new_content)
    console.print(f"[dim]✅ 元数据已更新: {OUTPUT_FILE}[/]")

def git_operations(version):
    tag_name = f"v{version}"
    console.rule(f"[bold cyan]🚀 第二步：发布 {tag_name}[/]")
    
    if not Confirm.ask(f"❓ 确认将 [bold green]{tag_name}[/] 推送到 GitHub 吗?"):
        console.print("[bold red]🚫 操作已取消[/]")
        sys.exit(0)

    console.print("[bold blue]📦 正在提交代码...[/]")
    
    # 1. 提交代码 (强制 add pom.xml 和 .zenodo.json)
    subprocess.run(f"git add {OUTPUT_FILE} pom.xml", shell=True)
    subprocess.run(f'git commit -m "chore: release {tag_name}"', shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # 2. Git 操作序列
    git_steps = [
        ("git push origin main", False), 
        (f"git tag -d {tag_name}", True), # 允许失败
        (f"git push origin :refs/tags/{tag_name}", True), # 允许失败
        (f"git tag -a {tag_name} -m \"Release {tag_name}\"", False),
        (f"git push origin {tag_name}", False)
    ]

    run_sequence_in_window(git_steps, "执行 Git 推送与打标...", "Git 发布 完成")

    console.print(Panel.fit(
        f"[bold green]🎉 RIA-J 发布成功！[/]\n\n"
        f"版本号: [bold cyan]{tag_name}[/]\n"
        f"下一步: 请前往 GitHub Releases 页面基于此 Tag 发布 Release。",
        title="完成",
        border_style="green"
    ))

if __name__ == "__main__":
    console.print(Panel.fit("[bold white]RIA-J 自动化发布工具[/] [dim](v3.1)[/]", style="bold blue"))
    ver = get_pom_version()
    final_ver = ask_for_version(ver)
    
    generate_zenodo_json(final_ver)
    build_project()
    git_operations(final_ver)