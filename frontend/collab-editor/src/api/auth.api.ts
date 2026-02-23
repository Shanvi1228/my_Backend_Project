import client from './client';
import type { ApiResponse } from '../types/common.types';
import type { LoginRequest, RegisterRequest, AuthResponse } from '../types/auth.types';

export const login = (req: LoginRequest): Promise<AuthResponse> =>
  client.post<ApiResponse<AuthResponse>>('/auth/login', req).then(r => r.data.data);

export const register = (req: RegisterRequest): Promise<AuthResponse> =>
  client.post<ApiResponse<AuthResponse>>('/auth/register', req).then(r => r.data.data);