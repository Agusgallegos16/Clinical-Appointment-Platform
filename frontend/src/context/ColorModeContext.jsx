import React, { createContext, useMemo, useState, useContext } from 'react';

const ColorModeContext = createContext({ toggleColorMode: () => {}, mode: 'light' });

export const useColorMode = () => useContext(ColorModeContext);

export const ColorModeProvider = ({ children }) => {
  const [mode, setMode] = useState(() => {
    return localStorage.getItem('theme_mode') || 'light';
  });

  const colorMode = useMemo(
    () => ({
      toggleColorMode: () => {
        setMode((prevMode) => {
          const nextMode = prevMode === 'light' ? 'dark' : 'light';
          localStorage.setItem('theme_mode', nextMode);
          return nextMode;
        });
      },
      mode,
    }),
    [mode]
  );

  return (
    <ColorModeContext.Provider value={colorMode}>
      {children}
    </ColorModeContext.Provider>
  );
};
