// src/context/AuthContext.tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import { changePasswordApi, loginApi, logoutApi, refreshApi } from '../api/auth';
import { setAccessToken, setOnUnauthorizedCallback } from '../api/client';
import { User } from '../types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, pass: string) => Promise<User>;
  logout: () => Promise<void>;
  changePassword: (oldPass: string, newPass: string) => Promise<void>;
  updateUserMustChangePassword: (mustChange: boolean) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Initialize session on mount via silent refresh
  useEffect(() => {
    setOnUnauthorizedCallback(() => {
      setUser(null);
      setAccessToken(null);
    });

    const checkSession = async () => {
      try {
        const refreshResult = await refreshApi();
        if (refreshResult.accessToken) {
          // Parse token claims or fetch user profile
          const parts = refreshResult.accessToken.split('.');
          if (parts.length === 3) {
            const payload = JSON.parse(atob(parts[1]));
            setUser({
              id: payload.sub,
              name: payload.name || 'User',
              email: payload.email || '',
              role: payload.role,
              enabled: true,
              mustChangePassword: payload.mustChangePassword || false,
            });
          }
        }
      } catch {
        setUser(null);
        setAccessToken(null);
      } finally {
        setIsLoading(false);
      }
    };

    checkSession();
  }, []);

  const login = async (email: string, pass: string): Promise<User> => {
    const res = await loginApi(email, pass);
    setUser(res.user);
    return res.user;
  };

  const logout = async () => {
    try {
      await logoutApi();
    } finally {
      setUser(null);
      setAccessToken(null);
    }
  };

  const changePassword = async (oldPass: string, newPass: string) => {
    await changePasswordApi(oldPass, newPass);
    if (user) {
      setUser({ ...user, mustChangePassword: false });
    }
  };

  const updateUserMustChangePassword = (mustChange: boolean) => {
    if (user) {
      setUser({ ...user, mustChangePassword: mustChange });
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        changePassword,
        updateUserMustChangePassword,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
