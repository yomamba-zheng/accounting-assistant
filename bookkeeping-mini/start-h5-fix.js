// start-h5-fix.js
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

console.log('🚀 正在启动 uni-app H5 (带全局变量修复)...');

// 1. 自动创建/确保 init-global.js 存在
const patchScriptPath = path.resolve(__dirname, 'init-global.js');
if (!fs.existsSync(patchScriptPath)) {
  const content = `
    const g = typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : {});
    if (!g.uniPlugin) {
      g.uniPlugin = { 
        options: {}, 
        defaultMode: 'component', 
        extNames: {}, 
        plugins: [] 
      };
      if (typeof global !== 'undefined') global.uniPlugin = g.uniPlugin;
    }
    // console.log('[Internal] uniPlugin injected');
  `;
  fs.writeFileSync(patchScriptPath, content);
}

// 2. 寻找 uni 的真实 JS 入口
// 优先级：@dcloudio/uni-cli/bin/uni.js > @dcloudio/uni-cli/index.js
const possibleUniPaths = [
  './node_modules/@dcloudio/uni-cli/bin/uni.js',
  './node_modules/@dcloudio/uni-cli/index.js',
  './node_modules/@dcloudio/uni-cli/lib/cli.js'
];

let uniEntryPath = null;
for (const p of possibleUniPaths) {
  const fullPath = path.resolve(__dirname, p);
  if (fs.existsSync(fullPath)) {
    uniEntryPath = fullPath;
    break;
  }
}

if (!uniEntryPath) {
  console.error('❌ 无法找到 uni-cli 的 JavaScript 入口文件。');
  console.error('尝试路径:', possibleUniPaths);
  console.error('💡 提示：请确认是否安装了 @dcloudio/uni-cli (npm list @dcloudio/uni-cli)');
  
  // 备选方案：如果找不到 uni-cli，尝试直接调用 vite-plugin-uni 的 bin (某些新版本)
  const vitePluginUniPath = path.resolve(__dirname, './node_modules/vite-plugin-uni/bin/uni.js');
  if (fs.existsSync(vitePluginUniPath)) {
    uniEntryPath = vitePluginUniPath;
    console.log(`✅ 备选方案生效：找到 vite-plugin-uni 入口: ${uniEntryPath}`);
  } else {
    process.exit(1);
  }
} else {
  console.log(`✅ 找到 uni-cli 入口: ${uniEntryPath}`);
}

// 3. 构造命令: node --require ./init-global.js <uni-entry.js> -p h5
const nodePath = process.execPath;
const args = ['--require', patchScriptPath, uniEntryPath, '-p', 'h5'];

console.log(`🔧 执行: ${nodePath} ${args.join(' ')}`);

// 4. 启动子进程
const child = spawn(nodePath, args, {
  stdio: 'inherit', // 继承终端输出
  shell: false      // 不需要 shell，直接传 JS 文件给 node
});

child.on('close', (code) => {
  if (code !== 0) {
    console.log(`⚠️ 进程退出，代码: ${code}`);
  } else {
    console.log('✅ 开发服务器已停止。');
  }
});