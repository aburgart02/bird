import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography, Alert } from '@mui/material';
import { authService } from '../services/api';

export default function GitHubCallback() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const requestSent = useRef(false);

    useEffect(() => {
        const code = searchParams.get('code');

        if (!code) {
            setError('Код авторизации не получен от GitHub');
            return;
        }

        if (requestSent.current)
            return;
        requestSent.current = true;

        const handleCallback = async () => {
            try {
                const response = await authService.githubLogin(code);
                if (response.data.token) {
                    localStorage.setItem('token', response.data.token);
                    navigate('/', { replace: true });
                } else {
                    setError('Ошибка авторизации через GitHub');
                }
            } catch (err) {
                setError(err.response?.data?.message || 'Ошибка при авторизации через GitHub');
            }
        };

        handleCallback().then();
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
                            onClick={() => navigate('/login')}
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
