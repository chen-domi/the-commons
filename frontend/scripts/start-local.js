// React Scripts can produce an undefined allowedHost when LAN detection fails.
// Use an explicit loopback-only development server and retain host validation.
process.env.HOST = '127.0.0.1';
process.env.WDS_SOCKET_HOST = 'localhost';
process.env.NODE_ENV = 'development';
process.env.BABEL_ENV = 'development';

const configPath = require.resolve('react-scripts/config/webpackDevServer.config');
const createConfig = require(configPath);

require.cache[configPath].exports = (proxy) => {
  // Browser navigations accept HTML, so CRA's default proxy skips OAuth URLs.
  const loginProxy = proxy && proxy.map((entry) => ({
    ...entry,
    context: (pathname, request) =>
      pathname.startsWith('/oauth2/') ||
      pathname.startsWith('/login/oauth2/') ||
      entry.context(pathname, request),
  }));
  return {
    ...createConfig(loginProxy, 'localhost'),
    allowedHosts: ['localhost', '127.0.0.1'],
  };
};

require('react-scripts/scripts/start');
