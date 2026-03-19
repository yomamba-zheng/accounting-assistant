
    const g = typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : {});
    if (!g.uniPlugin) {
      g.uniPlugin = { options: {}, defaultMode: 'component', extNames: {}, plugins: [] };
      if (typeof global !== 'undefined') global.uniPlugin = g.uniPlugin;
    }
    // console.log('[Internal Fix] uniPlugin injected');
  