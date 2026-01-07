import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography, Alert } from '@mui/material';
import { authService } from '../services/api';

export default function GitHubCallback() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [error, setError] = useState('');

    useEffect(() => {
        const code = searchParams.get('code');

        if (!code) {
            setError('Код авторизации не получен от GitHub');
            return;
        }

        const handleCallback = async () => {
            try {
                const response = await authService.githubLogin(code);
                if (response.data.token) {
                    localStorage.setItem('token', response.data.token);
                    navigate('/');
                } else if (response.data.code === '200') {
                    localStorage.setItem('token', response.data.data);
                    navigate('/');
                } else {
                    setError(response.data.message || 'Ошибка авторизации через GitHub');
                }
            } catch (err) {
                setError(err.response?.data?.message || 'Ошибка при авторизации через GitHub');
            }
        };

        handleCallback();
    }, [searchParams, navigate]);

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0f0f23 100%)',
            }}
        >
            {error ? (
                <Alert
                    severity="error"
                    sx={{ maxWidth: 400 }}
                    action={
                        <Typography
                            component="a"
                            href="/login"
                            sx={{ color: 'inherit', ml: 2 }}
                        >
                            Вернуться
                        </Typography>
                    }
                >
                    {error}
                </Alert>
            ) : (
                <>
                    <CircularProgress size={48} sx={{ color: 'primary.main', mb: 3 }} />
                    <Typography variant="h6" color="text.primary">
                        Авторизация через GitHub...
                    </Typography>
                </>
            )}
        </Box>
    );
}
