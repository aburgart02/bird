import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box,
    Card,
    CardContent,
    Typography,
    Button,
    Avatar,
    Chip,
    CircularProgress,
} from '@mui/material';
import { Logout, Person } from '@mui/icons-material';
import { authService } from '../services/api';

export default function Home() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            navigate('/login');
            return;
        }

        const fetchUser = async () => {
            try {
                const response = await authService.getCurrentUser();
                if (response.data.data) {
                    setUser(response.data.data);
                } else if (response.data.email) {
                    setUser(response.data);
                }
            } catch (err) {
                console.error('Failed to fetch user:', err);
                localStorage.removeItem('token');
                navigate('/login');
            } finally {
                setLoading(false);
            }
        };

        fetchUser();
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('token');
        navigate('/login');
    };

    if (loading) {
        return (
            <Box
                sx={{
                    minHeight: '100vh',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0f0f23 100%)',
                }}
            >
                <CircularProgress size={48} sx={{ color: 'primary.main' }} />
            </Box>
        );
    }

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #0f0f23 0%, #1a1a3e 50%, #0f0f23 100%)',
                position: 'relative',
                overflow: 'hidden',
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    width: '800px',
                    height: '800px',
                    background: 'radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%)',
                    top: '-400px',
                    right: '-400px',
                    borderRadius: '50%',
                },
                '&::after': {
                    content: '""',
                    position: 'absolute',
                    width: '600px',
                    height: '600px',
                    background: 'radial-gradient(circle, rgba(236, 72, 153, 0.1) 0%, transparent 70%)',
                    bottom: '-300px',
                    left: '-300px',
                    borderRadius: '50%',
                },
            }}
        >
            <Card
                sx={{
                    width: '100%',
                    maxWidth: 500,
                    mx: 2,
                    position: 'relative',
                    zIndex: 1,
                }}
            >
                <CardContent sx={{ p: 4, textAlign: 'center' }}>
                    <Typography
                        variant="h4"
                        component="h1"
                        sx={{
                            background: 'linear-gradient(135deg, #6366f1 0%, #ec4899 100%)',
                            backgroundClip: 'text',
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent',
                            mb: 4,
                        }}
                    >
                        🐦 Bird
                    </Typography>

                    <Avatar
                        sx={{
                            width: 100,
                            height: 100,
                            mx: 'auto',
                            mb: 3,
                            bgcolor: 'primary.main',
                            fontSize: '2.5rem',
                        }}
                    >
                        {user?.name?.charAt(0)?.toUpperCase() || <Person fontSize="large" />}
                    </Avatar>

                    <Typography variant="h5" sx={{ mb: 1, fontWeight: 600 }}>
                        Добро пожаловать, {user?.name || 'Пользователь'}!
                    </Typography>

                    <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                        {user?.email}
                    </Typography>

                    {user?.roles && user.roles.length > 0 && (
                        <Box sx={{ mb: 3 }}>
                            {user.roles.map((role, index) => (
                                <Chip
                                    key={index}
                                    label={role.role || role.name || role}
                                    color="primary"
                                    variant="outlined"
                                    size="small"
                                    sx={{ m: 0.5 }}
                                />
                            ))}
                        </Box>
                    )}

                    <Button
                        variant="outlined"
                        startIcon={<Logout />}
                        onClick={handleLogout}
                        sx={{
                            borderColor: 'rgba(255, 255, 255, 0.2)',
                            color: 'text.primary',
                            '&:hover': {
                                borderColor: 'error.main',
                                color: 'error.main',
                            },
                        }}
                    >
                        Выйти
                    </Button>
                </CardContent>
            </Card>
        </Box>
    );
}
