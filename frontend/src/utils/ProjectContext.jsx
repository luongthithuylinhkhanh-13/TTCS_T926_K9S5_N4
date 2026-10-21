import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { getProjects } from '../services/wbsApi';
import { getAuthUser, updateAuthUser } from './auth';
import apiClient from '../services/apiClient';

const ProjectContext = createContext(null);

const ACTIVE_PROJECT_KEY = 'constructflow_active_project_id';

export const ProjectProvider = ({ children }) => {
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [projects, setProjects] = useState([]);
  const [loadingProjects, setLoadingProjects] = useState(false);
  const [activeProjectId, setActiveProjectIdState] = useState(() => {
    // 1. URL search param
    try {
      const urlParam = new URLSearchParams(window.location.search).get('projectId');
      if (urlParam) return urlParam;
    } catch (e) {
      // ignore
    }
    // 2. localStorage
    try {
      const saved = localStorage.getItem(ACTIVE_PROJECT_KEY);
      if (saved) return saved;
    } catch (e) {
      // ignore
    }
    return null;
  });

  const [authUser, setAuthUser] = useState(getAuthUser);

  // Sync activeProjectId with URL param if it changes
  useEffect(() => {
    const queryParam = searchParams.get('projectId');
    if (queryParam && queryParam !== activeProjectId) {
      setActiveProjectIdState(queryParam);
      try {
        localStorage.setItem(ACTIVE_PROJECT_KEY, queryParam);
      } catch (e) {
        // ignore
      }
    }
  }, [searchParams, activeProjectId]);

  // Refresh user profile & memberships from backend
  const refreshMemberships = useCallback(async () => {
    const current = getAuthUser();
    if (!current?.token) return;
    try {
      const res = await apiClient.get('/api/auth/me');
      if (res.data?.success) {
        const updated = {
          ...current,
          ...res.data,
          projectMemberships: res.data.projectMemberships || []
        };
        updateAuthUser(updated);
        setAuthUser(updated);
        return updated;
      }
    } catch (err) {
      console.warn('Could not refresh project memberships:', err);
    }
  }, []);

  // Fetch projects on mount or when user changes
  const loadProjects = useCallback(async () => {
    const user = getAuthUser();
    if (!user?.token) return;
    setLoadingProjects(true);
    try {
      const data = await getProjects();
      const list = Array.isArray(data) ? data : [];
      setProjects(list);

      // Auto-select project logic:
      // If user has only 1 project, auto-select it
      if (list.length === 1) {
        const singleId = list[0].id;
        setActiveProjectIdState(singleId);
        try {
          localStorage.setItem(ACTIVE_PROJECT_KEY, singleId);
        } catch (e) {
          // ignore
        }
      } else if (list.length > 1) {
        // If current activeProjectId is valid in list, keep it
        setActiveProjectIdState(prev => {
          if (prev && list.some(p => p.id === prev)) {
            return prev;
          }
          // Check localStorage
          try {
            const saved = localStorage.getItem(ACTIVE_PROJECT_KEY);
            if (saved && list.some(p => p.id === saved)) {
              return saved;
            }
          } catch (e) {
            // ignore
          }
          // Default to first project
          const firstId = list[0].id;
          try {
            localStorage.setItem(ACTIVE_PROJECT_KEY, firstId);
          } catch (e) {
            // ignore
          }
          return firstId;
        });
      }
    } catch (err) {
      console.warn('Could not load projects in ProjectContext:', err);
    } finally {
      setLoadingProjects(false);
    }
  }, []);

  useEffect(() => {
    loadProjects();
    refreshMemberships();
  }, [loadProjects, refreshMemberships]);

  const setActiveProjectId = useCallback((id) => {
    setActiveProjectIdState(id);
    try {
      if (id) {
        localStorage.setItem(ACTIVE_PROJECT_KEY, id);
      } else {
        localStorage.removeItem(ACTIVE_PROJECT_KEY);
      }
    } catch (e) {
      // ignore
    }
  }, []);

  const activeProject = useMemo(() => {
    return projects.find(p => p.id === activeProjectId) || null;
  }, [projects, activeProjectId]);

  // Determine current project role and effective role
  const { currentProjectRole, effectiveRole } = useMemo(() => {
    const user = authUser || getAuthUser();
    const globalRole = String(user?.roleName || '').toUpperCase();

    // 1. ADMIN is a system exception: never overridden by project role
    if (globalRole === 'ADMIN') {
      return { currentProjectRole: 'ADMIN', effectiveRole: 'ADMIN' };
    }

    const memberships = user?.projectMemberships || [];

    // 2. Find membership for activeProjectId
    let matchedMembership = null;
    if (activeProjectId) {
      matchedMembership = memberships.find(
        m => m.projectId === activeProjectId && String(m.status || '').toUpperCase() === 'ACTIVE'
      );
    }

    // 3. If not found by activeProjectId, but user only has 1 active project membership, use that
    if (!matchedMembership && memberships.length === 1) {
      matchedMembership = memberships[0];
    }

    const projRole = matchedMembership?.roleName
      ? String(matchedMembership.roleName).toUpperCase()
      : null;

    // 4. Effective role: use project role if available, otherwise fallback to global role
    const effRole = projRole || globalRole || 'VIEWER';

    return {
      currentProjectRole: projRole,
      effectiveRole: effRole
    };
  }, [authUser, activeProjectId]);

  const value = useMemo(() => ({
    projects,
    loadingProjects,
    activeProjectId,
    activeProject,
    setActiveProjectId,
    currentProjectRole,
    effectiveRole,
    projectMemberships: authUser?.projectMemberships || [],
    refreshMemberships,
    loadProjects
  }), [
    projects,
    loadingProjects,
    activeProjectId,
    activeProject,
    setActiveProjectId,
    currentProjectRole,
    effectiveRole,
    authUser,
    refreshMemberships,
    loadProjects
  ]);

  return (
    <ProjectContext.Provider value={value}>
      {children}
    </ProjectContext.Provider>
  );
};

export const useProjectContext = () => {
  const context = useContext(ProjectContext);
  if (!context) {
    const user = getAuthUser();
    const globalRole = String(user?.roleName || '').toUpperCase();
    return {
      projects: [],
      loadingProjects: false,
      activeProjectId: null,
      activeProject: null,
      setActiveProjectId: () => {},
      currentProjectRole: null,
      effectiveRole: globalRole || 'VIEWER',
      projectMemberships: user?.projectMemberships || [],
      refreshMemberships: async () => {},
      loadProjects: async () => {}
    };
  }
  return context;
};

export default ProjectContext;
