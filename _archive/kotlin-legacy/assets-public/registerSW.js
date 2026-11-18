if ('serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then(function (regs) {
    for (var i = 0; i < regs.length; i++) {
      regs[i].unregister();
    }
  });
}
if (typeof caches !== 'undefined') {
  caches.keys().then(function (keys) {
    for (var j = 0; j < keys.length; j++) {
      caches.delete(keys[j]);
    }
  });
}
