import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'bootstrap/dist/css/bootstrap.min.css';
import LoginPage from './pages/login/LoginPage'
import IndexPage from './pages/index/IndexPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import './index.css';
import PageWrapperComponent from './pages/PageWrapperComponent';
import {
  createBrowserRouter,
  RouterProvider,
} from "react-router";
import { SnackbarProvider } from 'notistack';



let router = createBrowserRouter([
  {
    path: "/",
    Component: DashboardPage
  },
  {
    path: "/login",
    element: <LoginPage></LoginPage>
  }
]);


createRoot(document.getElementById('root')).render(
  <StrictMode>
    <SnackbarProvider>
      <RouterProvider router={router} />
    </SnackbarProvider>
  </StrictMode>,
)
