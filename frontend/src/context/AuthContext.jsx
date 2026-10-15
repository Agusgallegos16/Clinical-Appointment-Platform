import React, { createContext, useState, useContext } from 'react';
import { authService } from '../api/authService';

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user_data');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [loading, setLoading] = useState(false);

  const login = async (credentials) => {
    setLoading(true);
    try {
      const response = await authService.login(credentials);
      const userData = {
        id: response.id,
        email: response.email,
        rol: response.rol,
        entidadId: response.entidadId,
        token: response.token,
      };

      localStorage.setItem('jwt_token', response.token);
      localStorage.setItem('user_data', JSON.stringify(userData));
      setUser(userData);
      return userData;
    } finally {
      setLoading(false);
    }
  };

  const loginWithToken = async (token) => {
    setLoading(true);
    try {
      localStorage.setItem('jwt_token', token);
      const meResponse = await authService.getCurrentUser();
      const userData = {
        id: meResponse.id,
        email: meResponse.email,
        rol: meResponse.rol,
        entidadId: meResponse.entidadId,
        token: token,
      };

      localStorage.setItem('user_data', JSON.stringify(userData));
      setUser(userData);
      return userData;
    } catch (err) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user_data');
      setUser(null);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const updateUser = (newUserData) => {
    const updated = { ...user, ...newUserData };
    localStorage.setItem('user_data', JSON.stringify(updated));
    setUser(updated);
  };

  const logout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_data');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role: user?.rol || null,
        entidadId: user?.entidadId || null,
        isAuthenticated: !!user,
        loading,
        login,
        loginWithToken,
        updateUser,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
