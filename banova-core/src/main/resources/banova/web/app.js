/* Banova web panel — no build step, no external libraries. */
(() => {
  'use strict';

  // ------------------------------------------------------------------ i18n
  const I18N = {
    en: {
      workspace: 'Workspace',
      manage: 'Administration',
      shortcuts: 'My shortcuts',
      newShortcut: 'Add shortcut',
      saveView: 'Save view',
      shortcutName: 'Shortcut name',
      destination: 'Destination',
      filteredView: 'Current filters',
      review: 'Review action',
      deleteShortcut: 'Remove shortcut',
      activeCases: 'Active sanctions',
      storageFailed: 'Your browser could not save this shortcut.',
      inbox: 'Case log',
      configuration: 'Rules & settings',
      dashboard: 'Dashboard', punishments: 'Punishments', players: 'Players', appeals: 'Appeals', audit: 'Audit log',
      sessions: 'Sessions', logout: 'Log out', language: 'Language',
      loginTitle: 'Log in to Banova', loginText: 'Run this command in game or in the server console:',
      copy: 'Copy', copied: 'Copied', waiting: 'Waiting for approval…', remember: 'Keep me logged in',
      newCode: 'Get a new code', codeExpired: 'This code expired.', loginHint: 'Or use /banova web in game for a one-click link.',
      linkInvalid: 'That login link is invalid or expired.',
      activeBans: 'Active bans', activeMutes: 'Active mutes', activeWarns: 'Active warnings', today: 'Today', week: 'Last 7 days',
      pendingAppeals: 'Pending appeals', knownPlayers: 'Known players', online: 'Online',
      recent: 'Latest punishments', viewAll: 'View all', noPending: 'No pending appeals',
      lockdown: 'Lockdown', lockdownOn: 'The server is closed: only staff can join.', lockdownOff: 'The server is open.',
      enableLockdown: 'Close server', disableLockdown: 'Open server', lockdownReason: 'Reason (optional)',
      type: 'Type', player: 'Player', reason: 'Reason', staff: 'Staff', date: 'Date', duration: 'Duration', status: 'Status',
      expires: 'Expires', remaining: 'Time left', server: 'Server', id: 'ID', all: 'All', activeOnly: 'Active only',
      search: 'Search name, reason or #id…', newPunishment: 'New punishment', nothing: 'Nothing to show',
      page: 'Page {p} of {n}', prev: 'Previous', next: 'Next', loadMore: 'Load more',
      ban: 'Ban', mute: 'Mute', warn: 'Warning', kick: 'Kick',
      details: 'Details', revoke: 'Remove', edit: 'Edit', evidence: 'Evidence', addEvidence: 'Add evidence',
      evidencePlaceholder: 'Link to a screenshot/video, or a short text', add: 'Add', delete: 'Delete', cancel: 'Cancel',
      save: 'Save', confirm: 'Confirm', removedBy: 'Removed by', removeReason: 'Why removed', appealCode: 'Appeal code',
      silent: 'Silent', ipBased: 'IP punishment', template: 'Template', step: 'Step', noEvidence: 'No evidence attached.',
      revokeTitle: 'Remove punishment', revokeReason: 'Why are you removing it?', editTitle: 'Edit punishment',
      editDurationHint: 'Counted from the original date. Examples: 7d, 12h, perm. Leave empty to keep it.',
      target: 'Player name or UUID', mode: 'Mode', custom: 'Custom', reasonPlaceholder: 'Leave empty for the default reason',
      durationPlaceholder: '1d, 12h, 30m, 1mo or perm', permanent: 'Permanent', punish: 'Punish',
      silentHint: 'Only staff are told', ipHint: 'Also punish every account on the same IP',
      searchPlayers: 'Search players by name…', recentPlayers: 'Recently seen players', firstSeen: 'First seen', lastSeen: 'Last seen',
      onlineNow: 'Online now', banned: 'Banned', muted: 'Muted', warnings: 'Active warnings', history: 'History',
      notes: 'Private notes', addNote: 'Add a note only staff can see…', alts: 'Accounts on the same IP', ips: 'IP history',
      quickPunish: 'Quick punish (automatic escalation)', nextStep: 'Next', offenses: 'previous offenses',
      noNotes: 'No notes yet.', noAlts: 'No other accounts found.', applyTemplate: 'Apply {name}?',
      pending: 'Pending', accepted: 'Accepted', denied: 'Denied', message: 'Message', contact: 'Contact',
      accept: 'Accept and remove punishment', deny: 'Deny', response: 'Reply to the player (optional)',
      previousAppeals: 'Previous appeals', handledBy: 'Handled by', punishment: 'Punishment', sent: 'Sent',
      action: 'Action', target2: 'Target', source: 'Source', filterStaff: 'Filter by staff name…',
      browser: 'Browser', ip: 'IP', lastUsed: 'Last used', current: 'This browser', persistent: 'Remembered',
      endSession: 'Log out', error: 'Something went wrong', forbidden: 'You do not have permission for that.',
      created: 'Created', sessionExpired: 'Your session expired, please log in again.'
    },
    es: {
      workspace: 'Moderación',
      manage: 'Administración',
      shortcuts: 'Mis atajos',
      newShortcut: 'Añadir atajo',
      saveView: 'Guardar vista',
      shortcutName: 'Nombre del atajo',
      destination: 'Destino',
      filteredView: 'Filtros actuales',
      review: 'Revisar acción',
      deleteShortcut: 'Quitar atajo',
      activeCases: 'Sanciones activas',
      storageFailed: 'El navegador no pudo guardar el atajo.',
      inbox: 'Registro de sanciones',
      configuration: 'Rutas y ajustes',
      dashboard: 'Inicio', punishments: 'Sanciones', players: 'Jugadores', appeals: 'Apelaciones', audit: 'Auditoría',
      sessions: 'Sesiones', logout: 'Cerrar sesión', language: 'Idioma',
      loginTitle: 'Entrar en Banova', loginText: 'Escribe este comando en el juego o en la consola del servidor:',
      copy: 'Copiar', copied: 'Copiado', waiting: 'Esperando aprobación…', remember: 'Mantener la sesión iniciada',
      newCode: 'Obtener otro código', codeExpired: 'Este código ha caducado.', loginHint: 'O usa /banova web en el juego para entrar con un clic.',
      linkInvalid: 'Ese enlace de acceso no es válido o ha caducado.',
      activeBans: 'Baneos activos', activeMutes: 'Silencios activos', activeWarns: 'Avisos activos', today: 'Hoy', week: 'Últimos 7 días',
      pendingAppeals: 'Apelaciones pendientes', knownPlayers: 'Jugadores registrados', online: 'Conectados',
      recent: 'Últimas sanciones', viewAll: 'Ver todo', noPending: 'No hay apelaciones pendientes',
      lockdown: 'Cierre', lockdownOn: 'El servidor está cerrado: solo puede entrar el staff.', lockdownOff: 'El servidor está abierto.',
      enableLockdown: 'Cerrar servidor', disableLockdown: 'Abrir servidor', lockdownReason: 'Motivo (opcional)',
      type: 'Tipo', player: 'Jugador', reason: 'Motivo', staff: 'Staff', date: 'Fecha', duration: 'Duración', status: 'Estado',
      expires: 'Expira', remaining: 'Tiempo restante', server: 'Servidor', id: 'ID', all: 'Todo', activeOnly: 'Solo activas',
      search: 'Buscar nombre, motivo o #id…', newPunishment: 'Nueva sanción', nothing: 'No hay nada que mostrar',
      page: 'Página {p} de {n}', prev: 'Anterior', next: 'Siguiente', loadMore: 'Cargar más',
      ban: 'Baneo', mute: 'Silencio', warn: 'Aviso', kick: 'Expulsión',
      details: 'Detalles', revoke: 'Retirar', edit: 'Editar', evidence: 'Pruebas', addEvidence: 'Añadir prueba',
      evidencePlaceholder: 'Enlace a una captura/vídeo o un texto corto', add: 'Añadir', delete: 'Borrar', cancel: 'Cancelar',
      save: 'Guardar', confirm: 'Confirmar', removedBy: 'Retirada por', removeReason: 'Motivo de retirada', appealCode: 'Código de apelación',
      silent: 'Silenciosa', ipBased: 'Sanción por IP', template: 'Plantilla', step: 'Paso', noEvidence: 'No hay pruebas adjuntas.',
      revokeTitle: 'Retirar sanción', revokeReason: '¿Por qué la retiras?', editTitle: 'Editar sanción',
      editDurationHint: 'Se cuenta desde la fecha original. Ejemplos: 7d, 12h, perm. Déjalo vacío para no cambiarla.',
      target: 'Nombre del jugador o UUID', mode: 'Modo', custom: 'Personalizada', reasonPlaceholder: 'Vacío para el motivo por defecto',
      durationPlaceholder: '1d, 12h, 30m, 1mo o perm', permanent: 'Permanente', punish: 'Sancionar',
      silentHint: 'Solo se avisa al staff', ipHint: 'Sancionar también a las cuentas con la misma IP',
      searchPlayers: 'Buscar jugadores por nombre…', recentPlayers: 'Jugadores vistos recientemente', firstSeen: 'Primera vez', lastSeen: 'Última vez',
      onlineNow: 'Conectado ahora', banned: 'Baneado', muted: 'Silenciado', warnings: 'Avisos activos', history: 'Historial',
      notes: 'Notas privadas', addNote: 'Añade una nota que solo verá el staff…', alts: 'Cuentas con la misma IP', ips: 'Historial de IPs',
      quickPunish: 'Sanción rápida (escalado automático)', nextStep: 'Siguiente', offenses: 'infracciones previas',
      noNotes: 'Todavía no hay notas.', noAlts: 'No hay otras cuentas.', applyTemplate: '¿Aplicar {name}?',
      pending: 'Pendientes', accepted: 'Aceptadas', denied: 'Rechazadas', message: 'Mensaje', contact: 'Contacto',
      accept: 'Aceptar y retirar la sanción', deny: 'Rechazar', response: 'Respuesta para el jugador (opcional)',
      previousAppeals: 'Apelaciones anteriores', handledBy: 'Revisada por', punishment: 'Sanción', sent: 'Enviada',
      action: 'Acción', target2: 'Objetivo', source: 'Origen', filterStaff: 'Filtrar por nombre del staff…',
      browser: 'Navegador', ip: 'IP', lastUsed: 'Último uso', current: 'Este navegador', persistent: 'Recordada',
      endSession: 'Cerrar', error: 'Algo ha fallado', forbidden: 'No tienes permiso para eso.',
      created: 'Creada', sessionExpired: 'Tu sesión ha caducado, vuelve a entrar.'
    }
  };
  // Rules & settings page
  Object.assign(I18N.en, {
    rulesTab: 'Punishment routes', settingsTab: 'Server settings',
    menuLayout: 'In-game menu', menuLayoutHint: 'Drag routes to reorder them or move them to another row. Staff see the routes in this order in /punish. Click a route to edit it.',
    catChat: 'Chat & conduct', catGameplay: 'Fair play', catAccount: 'Account', perPage: '6 per page',
    newRoute: 'New route', stepsCount: '{n} steps', moveHint: 'Drag, or use the arrow keys to move it',
    editingRoute: 'Editing route', creatingRoute: 'New route', routeName: 'Route name', routeNamePh: 'Route name, e.g. Cheating',
    routeReason: 'Reason shown to the player', routeReasonPh: 'e.g. Using hacked clients or unfair modifications',
    escalation: 'Escalation', escalationHint: 'Drag a punishment into the list, or click it to add it at the end. Drag the handle to reorder.',
    addStep: 'Add a step', paletteHint: 'Drag into the list or click to add', offense: '{n} offense',
    repeatsNote: 'Further offenses repeat the last step.', removeStep: 'Remove step', oneStep: 'A route needs at least one step.',
    kickNow: 'Disconnects the player', warnActive: 'Counts as active for {d}', badDuration: 'Use 30m, 12h, 7d, 1mo or perm',
    resetAfter: 'Forget old offenses after', resetHint: 'Offenses older than this no longer count, so the player starts again at the 1st step.',
    never: 'Never', customValue: 'Custom value', moreOptions: 'More options', menuRow: 'Menu row', menuItem: 'Menu item (Minecraft material)',
    routeId: 'ID (used in commands)', routeIdHint: 'Created from the name. It cannot be changed later.', aliases: 'Command aliases',
    aliasesPh: 'hacks, cheats', permission: 'Required permission (optional)', silentRoute: 'Always silent (only staff are told)',
    unsaved: 'Unsaved changes', allSaved: 'All changes saved', newRouteHint: 'New route, not saved yet', discard: 'Discard',
    saveRoute: 'Save route', createRoute: 'Create route', deleteRoute: 'Delete', routeSaved: 'Route saved', routeDeleted: 'Route deleted',
    deleteRouteText: 'Staff will no longer see this route. Punishments already given keep their history.',
    filterNote: 'The chat filter uses this route for automatic punishments.',
    usedByFilter: 'The chat filter uses this route. Change auto-punish.template in filter.yml before deleting it.',
    layoutSaved: 'Menu order saved', fileChanged: 'The file was changed somewhere else. The page has been reloaded.',
    invalidFields: 'Check the highlighted fields.', idTaken: 'A route with that ID already exists.',
    discardTitle: 'Discard unsaved changes?', discardText: 'The changes you made on this page will be lost.',
    langSetting: 'Language', langSettingHint: 'In-game messages, ban screens, the appeal page and the default language of this panel. Each staff member can still switch the panel language.',
    langNote: 'Route names and reasons live in templates.yml and are not translated automatically.',
    broadcastSetting: 'Punishment announcements', broadcastHint: 'Who sees the public message when someone is punished.',
    bcAll: 'Everyone', bcAllHint: 'Every player sees who was punished and why.', bcStaff: 'Staff only', bcStaffHint: 'Only players with banova.notify.',
    bcNone: 'Nobody', bcNoneHint: 'No announcements. The punished player is still told.',
    behaviour: 'Behaviour', silentDefault: 'Silent by default', silentDefaultHint: 'Punishments only notify staff unless -p is added.',
    evasion: 'Ban evasion alerts', evasionHint: 'Tell staff when someone joins sharing an IP with a banned account.',
    appealsSetting: 'Appeals', appealsSettingHint: 'Players can appeal bans and mutes from the web page or with /appeal.',
    saveSettings: 'Save settings', settingsSaved: 'Settings saved'
  });
  Object.assign(I18N.es, {
    rulesTab: 'Rutas de sanción', settingsTab: 'Ajustes del servidor',
    menuLayout: 'Menú del juego', menuLayoutHint: 'Arrastra las rutas para ordenarlas o pasarlas a otra fila. El staff las ve en este orden en /punish. Haz clic en una ruta para editarla.',
    catChat: 'Chat y trato', catGameplay: 'Juego limpio', catAccount: 'Cuenta', perPage: '6 por página',
    newRoute: 'Nueva ruta', stepsCount: '{n} pasos', moveHint: 'Arrastra o usa las flechas del teclado para moverla',
    editingRoute: 'Editando ruta', creatingRoute: 'Nueva ruta', routeName: 'Nombre de la ruta', routeNamePh: 'Nombre de la ruta, p. ej. Hacks',
    routeReason: 'Motivo que ve el jugador', routeReasonPh: 'p. ej. Usar clientes modificados o ventajas injustas',
    escalation: 'Escalado', escalationHint: 'Arrastra una sanción a la lista o haz clic para añadirla al final. Arrastra el asa para reordenar.',
    addStep: 'Añadir un paso', paletteHint: 'Arrástrala a la lista o haz clic para añadirla', offense: '{n} infracción',
    repeatsNote: 'Las siguientes infracciones repiten el último paso.', removeStep: 'Quitar paso', oneStep: 'Una ruta necesita al menos un paso.',
    kickNow: 'Desconecta al jugador', warnActive: 'Cuenta como activo durante {d}', badDuration: 'Usa 30m, 12h, 7d, 1mo o perm',
    resetAfter: 'Olvidar infracciones antiguas tras', resetHint: 'Las infracciones más antiguas dejan de contar y el jugador vuelve al 1.er paso.',
    never: 'Nunca', customValue: 'Valor personalizado', moreOptions: 'Más opciones', menuRow: 'Fila del menú', menuItem: 'Objeto del menú (material de Minecraft)',
    routeId: 'ID (se usa en comandos)', routeIdHint: 'Se crea a partir del nombre. No se puede cambiar después.', aliases: 'Alias de comando',
    aliasesPh: 'hacks, trampas', permission: 'Permiso necesario (opcional)', silentRoute: 'Siempre silenciosa (solo se avisa al staff)',
    unsaved: 'Cambios sin guardar', allSaved: 'Todo guardado', newRouteHint: 'Ruta nueva, aún sin guardar', discard: 'Descartar',
    saveRoute: 'Guardar ruta', createRoute: 'Crear ruta', deleteRoute: 'Borrar', routeSaved: 'Ruta guardada', routeDeleted: 'Ruta borrada',
    deleteRouteText: 'El staff dejará de ver esta ruta. Las sanciones ya puestas conservan su historial.',
    filterNote: 'El filtro de chat usa esta ruta para las sanciones automáticas.',
    usedByFilter: 'El filtro de chat usa esta ruta. Cambia auto-punish.template en filter.yml antes de borrarla.',
    layoutSaved: 'Orden del menú guardado', fileChanged: 'El archivo se ha cambiado en otro sitio. La página se ha recargado.',
    invalidFields: 'Revisa los campos marcados.', idTaken: 'Ya existe una ruta con ese ID.',
    discardTitle: '¿Descartar los cambios sin guardar?', discardText: 'Se perderán los cambios que has hecho en esta página.',
    langSetting: 'Idioma', langSettingHint: 'Mensajes del juego, pantallas de baneo, la página de apelaciones y el idioma por defecto de este panel. Cada miembro del staff puede cambiar el idioma del panel.',
    langNote: 'Los nombres y motivos de las rutas están en templates.yml y no se traducen solos.',
    broadcastSetting: 'Anuncios de sanciones', broadcastHint: 'Quién ve el mensaje público cuando se sanciona a alguien.',
    bcAll: 'Todos', bcAllHint: 'Todos los jugadores ven a quién se sancionó y por qué.', bcStaff: 'Solo staff', bcStaffHint: 'Solo jugadores con banova.notify.',
    bcNone: 'Nadie', bcNoneHint: 'Sin anuncios. El jugador sancionado sí recibe el aviso.',
    behaviour: 'Comportamiento', silentDefault: 'Silenciosas por defecto', silentDefaultHint: 'Las sanciones solo avisan al staff salvo que se añada -p.',
    evasion: 'Alertas de evasión de baneo', evasionHint: 'Avisa al staff cuando entra alguien que comparte IP con una cuenta baneada.',
    appealsSetting: 'Apelaciones', appealsSettingHint: 'Los jugadores pueden apelar baneos y silencios desde la web o con /appeal.',
    saveSettings: 'Guardar ajustes', settingsSaved: 'Ajustes guardados'
  });

  // English unless this browser picked a language or the server is set to Spanish (config.yml "language").
  const savedLang = () => {
    try { const v = localStorage.getItem('banova.lang'); return v === 'en' || v === 'es' ? v : null; } catch (e) { return null; }
  };
  let lang = savedLang() || 'en';
  const t = (key, vars) => {
    let s = (I18N[lang] && I18N[lang][key]) || I18N.en[key] || key;
    if (vars) for (const k of Object.keys(vars)) s = s.replace('{' + k + '}', vars[k]);
    return s;
  };
  const setLang = (l) => {
    lang = l;
    try { localStorage.setItem('banova.lang', l); } catch (e) { /* ignore */ }
    document.documentElement.lang = l;
    route();
  };

  // ------------------------------------------------------------------ DOM helpers
  function el(tag, attrs, ...children) {
    const node = document.createElement(tag);
    if (attrs) {
      for (const [k, v] of Object.entries(attrs)) {
        if (v === null || v === undefined || v === false) continue;
        if (k === 'class') node.className = v;
        else if (k === 'style') node.style.cssText = v; // CSSOM is allowed by our strict CSP, style attributes are not
        else if (k === 'text') node.textContent = v;
        else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.substring(2), v);
        else if (k === 'value') node.value = v;
        else if (k === 'checked') node.checked = !!v;
        else node.setAttribute(k, v === true ? '' : v);
      }
    }
    for (const c of children.flat(Infinity)) {
      if (c === null || c === undefined || c === false) continue;
      node.appendChild(c instanceof Node ? c : document.createTextNode(String(c)));
    }
    return node;
  }
  const $ = (sel) => document.querySelector(sel);
  const clear = (node) => { while (node.firstChild) node.removeChild(node.firstChild); return node; };
  const strip = (s) => (s == null ? '' : String(s).replace(/[&§](#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])/g, ''));
  const fmtDate = (ms) => (ms > 0 ? new Date(ms).toLocaleString(lang === 'es' ? 'es-ES' : 'en-GB',
    { dateStyle: 'medium', timeStyle: 'short' }) : '—');

  const ICONS = {
    pin: 'M6 3h12v18l-6-4-6 4z',
    settings: 'M4 7h8m4 0h4M4 17h2m4 0h10M12 4v6M7 14v6',
    arrow: 'M4 12h14m-5-5 5 5-5 5',
    close: 'M6 6l12 12M18 6L6 18',
    search: 'M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14m5 12 6 6',
    home: 'M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',
    gavel: 'M14 3l7 7-3 3-7-7zM9 8l7 7-2 2-7-7zM3 21l6-6',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8',
    mail: 'M4 4h16v16H4zM4 6l8 7 8-7',
    list: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
    key: 'M21 2l-2 2m-7.6 7.6a5.5 5.5 0 1 1-7.8 7.8 5.5 5.5 0 0 1 7.8-7.8zM15.5 7.5l3 3L22 7l-3-3',
    out: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9',
    menu: 'M3 6h18M3 12h18M3 18h18',
    plus: 'M12 5v14M5 12h14',
    grip: 'M8 6a1 1 0 1 0 2 0a1 1 0 1 0-2 0M14 6a1 1 0 1 0 2 0a1 1 0 1 0-2 0M8 12a1 1 0 1 0 2 0a1 1 0 1 0-2 0M14 12a1 1 0 1 0 2 0a1 1 0 1 0-2 0M8 18a1 1 0 1 0 2 0a1 1 0 1 0-2 0M14 18a1 1 0 1 0 2 0a1 1 0 1 0-2 0',
    repeat: 'M17 2l4 4-4 4M3 11v-1a4 4 0 0 1 4-4h14M7 22l-4-4 4-4M21 13v1a4 4 0 0 1-4 4H3',
    globe: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20'
  };
  const icon = (name) => {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', ICONS[name]);
    svg.appendChild(path);
    return svg;
  };

  // ------------------------------------------------------------------ API
  class ApiError extends Error {
    constructor(status, code) { super(code); this.status = status; this.code = code; }
  }
  async function api(method, path, body) {
    const opts = { method, credentials: 'same-origin', headers: { 'Accept': 'application/json' } };
    if (method !== 'GET') {
      opts.headers['Content-Type'] = 'application/json';
      opts.headers['X-Banova'] = '1';
      opts.body = JSON.stringify(body || {});
    }
    const res = await fetch('/api/' + path, opts);
    let data = null;
    try { data = await res.json(); } catch (e) { data = null; }
    if (res.status === 401 && !path.startsWith('auth/')) {
      // Only an already logged-in page has "expired"; on first load the caller shows the login screen.
      if (state.meta) {
        state.meta = null;
        toast(t('sessionExpired'), true);
        showLogin();
      }
      throw new ApiError(401, 'unauthorized');
    }
    if (!res.ok) throw new ApiError(res.status, (data && data.error) || 'error');
    return data;
  }
  const q = (params) => {
    const parts = [];
    for (const [k, v] of Object.entries(params)) if (v !== '' && v !== null && v !== undefined && v !== false) parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
    return parts.length ? '?' + parts.join('&') : '';
  };

  function toast(message, error) {
    const node = el('div', { class: 'toast' + (error ? ' error' : '') }, message);
    $('#toasts').appendChild(node);
    setTimeout(() => node.remove(), error ? 6000 : 4000);
  }
  function showResult(res) {
    const msgs = (res && res.messages) || [];
    if (msgs.length) msgs.forEach((m) => toast(m, !res.ok));
    else if (res && !res.ok) toast(t('error'), true);
  }
  function fail(e) {
    if (e && e.status === 401) return;
    toast(e && e.status === 403 ? t('forbidden') : t('error') + (e && e.code ? ' (' + e.code + ')' : ''), true);
  }

  // ------------------------------------------------------------------ state & helpers
  const state = { meta: null, pendingAppeals: 0, pollTimer: null };
  // A page with unsaved edits sets leaveGuard; navigation then asks before throwing the edits away.
  let leaveGuard = null;
  let currentHash = location.hash;
  const quietHash = (hash) => { history.replaceState(null, '', hash); currentHash = location.hash; };
  const can = (perm) => !!(state.meta && state.meta.user.permissions.includes(perm));
  const avatar = (uuid, big) => (state.meta && state.meta.avatars && uuid)
    ? el('img', { class: 'avatar' + (big ? ' big' : ''), src: 'https://mc-heads.net/avatar/' + uuid + '/' + (big ? 64 : 24), alt: '', loading: 'lazy' })
    : el('span', { class: 'avatar' + (big ? ' big' : '') });
  const typeBadge = (p) => el('span', { class: 'badge ' + p.type }, strip(p.typeName) + (p.ipBased ? ' IP' : ''));
  const statusBadge = (p) => el('span', { class: 'badge ' + p.status }, strip(p.statusName));
  const playerCell = (name, uuid) => el('span', { class: 'player-cell' }, avatar(uuid), name || '—');
  const go = (hash) => { location.hash = hash; };

  function verifyAction(action, detail) {
    return new Promise(resolve => {
      const dialog = el('dialog', {class:'review-dialog', 'aria-label':t('review')});
      const cancel = el('button', {class:'btn', type:'button', onclick:()=>dialog.close('cancel')}, t('cancel'));
      dialog.append(el('small',{class:'section-label'},t('review')), el('h2',{},action),
        el('p',{class:'review-detail'},detail), el('div',{class:'row end'},cancel,
          el('button',{class:'btn danger',type:'button',onclick:()=>dialog.close('approve')},t('confirm'))));
      const abandon = () => dialog.close('cancel');
      dialog.addEventListener('keydown', event => event.stopPropagation());
      dialog.addEventListener('close', () => {
        window.removeEventListener('hashchange', abandon);
        dialog.remove();
        resolve(dialog.returnValue === 'approve');
      }, {once:true});
      window.addEventListener('hashchange', abandon);
      document.body.append(dialog);
      dialog.showModal();
      cancel.focus();
    });
  }

  function modal(title, body, actions) {
    const root = $('#modal-root');
    clear(root);
    const close = () => clear(root);
    const backdrop = el('div', { class: 'modal-backdrop', onclick: (e) => { if (e.target === backdrop) close(); } },
      el('div', { class: 'card modal', role:'dialog', 'aria-modal':'true', 'aria-label':title }, el('h2', null, title), body,
        el('div', { class: 'row end', style: 'margin-top:14px' }, el('button', { class: 'btn', onclick: close }, t('cancel')), actions(close))));
    root.appendChild(backdrop);
    const first = backdrop.querySelector('input, textarea, select');
    if (first) first.focus();
    return close;
  }

  // ------------------------------------------------------------------ login
  function showLogin() {
    if (state.pollTimer) clearInterval(state.pollTimer);
    const app = clear($('#app'));
    let remember = true;
    const codeBox = el('div', { class: 'code' }, '········');
    const cmd = el('code', null, '/banova web login …');
    const status = el('div', { class: 'waiting' }, el('span', { class: 'spinner' }), t('waiting'));
    const copyBtn = el('button', { class: 'btn small', onclick: () => {
      navigator.clipboard && navigator.clipboard.writeText(cmd.textContent).then(() => { copyBtn.textContent = t('copied'); });
    } }, t('copy'));
    const start = async () => {
      if (state.pollTimer) clearInterval(state.pollTimer);
      copyBtn.textContent = t('copy');
      clear(status).append(el('span', { class: 'spinner' }), t('waiting'));
      let login;
      try { login = await api('POST', 'auth/start', { remember }); } catch (e) { fail(e); return; }
      codeBox.textContent = login.code;
      cmd.textContent = login.command;
      const started = Date.now();
      state.pollTimer = setInterval(async () => {
        if (Date.now() - started > 5 * 60 * 1000) {
          clearInterval(state.pollTimer);
          clear(status).append(t('codeExpired'), ' ', el('button', { class: 'btn small', onclick: start }, t('newCode')));
          return;
        }
        try {
          const res = await api('POST', 'auth/poll', { poll: login.poll });
          if (res.status === 'approved') { clearInterval(state.pollTimer); boot(); }
        } catch (e) { /* keep polling */ }
      }, 2000);
    };
    app.appendChild(el('div', { class: 'login' }, el('div', { class: 'card' },
      el('img', { src: '/assets/logo.png', alt: 'Banova' }),
      el('h1', null, t('loginTitle')),
      el('p', { class: 'muted' }, t('loginText')),
      codeBox,
      el('div', { class: 'command' }, cmd, copyBtn),
      status,
      el('div', { class: 'row', style: 'justify-content:center;margin-top:16px' },
        el('label', { class: 'check' }, el('input', { type: 'checkbox', checked: true, onchange: (e) => { remember = e.target.checked; start(); } }), t('remember'))),
      el('p', { class: 'muted', style: 'font-size:12px;margin-top:16px' }, t('loginHint')),
      el('div', { class: 'row', style: 'justify-content:center' }, langSwitch()))));
    start();
  }

  function langSwitch() {
    return el('div', { class: 'seg' },
      ['en', 'es'].map((l) => el('button', { class: l === lang ? 'active' : '', onclick: () => { setLang(l); if (!state.meta) showLogin(); } }, l.toUpperCase())));
  }

  // ------------------------------------------------------------------ layout
  const NAV = [
    { hash: '#/', key: 'dashboard', icon: 'home', perm: 'banova.history' },
    { hash: '#/punishments', key: 'punishments', icon: 'gavel', perm: 'banova.history' },
    { hash: '#/players', key: 'players', icon: 'users', perm: 'banova.history' },
    { hash: '#/appeals', key: 'appeals', icon: 'mail', perm: 'banova.appeals' },
    { hash: '#/audit', key: 'audit', icon: 'list', perm: 'banova.audit' },
    { hash: '#/configuration', key: 'configuration', icon: 'settings', perm: 'banova.config.edit' },
    { hash: '#/sessions', key: 'sessions', icon: 'key', perm: 'banova.web' }
  ];

  function layout(title, content, actions) {
    const app = clear($('#app'));
    const current = location.hash || '#/';
    const sidebar = el('aside', { class: 'sidebar' },
      el('a', { class: 'brand', href: '#/' }, el('img', { src: '/assets/logo.png', alt: '' }),
        el('div', null, 'Banova', el('small', null, strip(state.meta.server) + ' · v' + state.meta.version))),
      ...['workspace','manage'].map(group => {
        const entries = NAV.filter(n => can(n.perm) && (['sessions','configuration','audit'].includes(n.key) ? 'manage' : 'workspace') === group);
        if (!entries.length) return null;
        return el('nav', {class:'nav','aria-label':t(group)}, el('div',{class:'nav-label'},t(group)), entries.map(n => {
        const active = n.hash === '#/' ? current === '#/' || current === '' : current.startsWith(n.hash) ||
          (n.key === 'punishments' && current.startsWith('#/p/')) || (n.key === 'players' && current.startsWith('#/player/')) ||
          (n.key === 'appeals' && current.startsWith('#/appeal/'));
        return el('a', { href: n.hash, 'aria-current':active ? 'page' : null, class: active ? 'active' : '' }, icon(n.icon), t(n.key),
          n.key === 'appeals' && state.pendingAppeals > 0 ? el('span', { class: 'badge count' }, state.pendingAppeals) : null);
        }));
      }),
      el('div', { class: 'spacer' }),
      el('div', { class: 'row', style: 'padding:0 8px 10px' }, langSwitch()),
      el('div', { class: 'userbox' }, avatar(state.meta.user.uuid),
        el('div', null, el('div', { class: 'name' }, state.meta.user.name)),
        el('div', { class: 'actions' }, el('button', { class: 'btn ghost icon', title: t('logout'), onclick: logout }, icon('out')))));
    const main = el('main', { class: 'main' },
      el('div', { class: 'topbar' },
        el('button', { class: 'btn icon menu-toggle', onclick: () => sidebar.classList.toggle('open') }, icon('menu')),
        el('div',{class:'page-heading'},el('div',{class:'crumbs'},strip(state.meta.server),
          ...[['#/p/','punishments'],['#/player/','players'],['#/appeal/','appeals']].filter(([prefix])=>current.startsWith(prefix))
            .map(([,key])=>el('a',{href:'#/'+key},' / '+t(key)))),el('h1', null, title)), el('div', { class: 'grow' }),
        can('banova.history') ? el('button',{class:'btn quick-search', onclick:focusPlayerSearch},
          icon('search'), t('searchPlayers'), el('kbd',{},'/')) : null, actions || null),
      content);
    app.appendChild(el('div', { class: 'layout' }, sidebar, main));
  }

  function focusPlayerSearch() {
    if (location.hash.split('?')[0] === '#/players') document.querySelector('input[type=search]')?.focus();
    else go('#/players');
  }
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && $('#modal-root').firstChild) clear($('#modal-root'));
    if (event.key !== '/' || event.ctrlKey || event.metaKey || event.altKey || $('#modal-root').firstChild ||
        event.target.closest('input,textarea,select,[contenteditable]') || !state.meta || !can('banova.history')) return;
    event.preventDefault();
    focusPlayerSearch();
  });

  async function logout() {
    try { await api('POST', 'auth/logout', {}); } catch (e) { /* ignore */ }
    state.meta = null;
    showLogin();
  }

  function punishButton(prefill) {
    if (!['banova.ban', 'banova.tempban', 'banova.mute', 'banova.tempmute', 'banova.warn', 'banova.kick', 'banova.punish'].some(can)) return null;
    return el('button', { class: 'btn primary', onclick: () => punishModal(prefill) }, icon('plus'), t('newPunishment'));
  }

  // ------------------------------------------------------------------ tables
  function punishmentTable(items, opts = {}) {
    if (!items.length) return el('div', { class: 'empty' }, t('nothing'));
    return el('div', { class: 'table-wrap' }, el('table', null,
      el('thead', null, el('tr', null, el('th', null, t('id')), el('th', null, t('type')),
        opts.noPlayer ? null : el('th', null, t('player')), el('th', null, t('reason')),
        el('th', { class: 'hide-mobile' }, t('staff')), el('th', { class: 'hide-mobile' }, t('duration')),
        el('th', null, t('status')), el('th', { class: 'hide-mobile' }, t('date')))),
      el('tbody', null, items.map((p) => el('tr', { class: 'click', onclick: () => go('#/p/' + p.id) },
        el('td', { class: 'mono muted' }, el('a',{class:'case-number',href:'#/p/'+p.id},'#' + p.id)), el('td', null, typeBadge(p)),
        opts.noPlayer ? null : el('td', null, playerCell(p.targetName, p.targetUuid)),
        el('td', null, el('span', { class: 'reason', title: p.reason }, p.reason)),
        el('td', { class: 'hide-mobile' }, p.actorName), el('td', { class: 'hide-mobile' }, strip(p.durationText)),
        el('td', null, statusBadge(p), p.silent ? ' ' : null, p.silent ? el('span', { class: 'badge' }, t('silent')) : null),
        el('td', { class: 'hide-mobile muted date' }, fmtDate(p.createdAt)))))));
  }

  function pager(page, pages, onPage) {
    if (pages <= 1) return null;
    return el('div', { class: 'pager' },
      el('button', { class: 'btn small', disabled: page <= 1, onclick: () => onPage(page - 1) }, t('prev')),
      el('span', null, t('page', { p: page, n: pages })),
      el('button', { class: 'btn small', disabled: page >= pages, onclick: () => onPage(page + 1) }, t('next')));
  }

  // ------------------------------------------------------------------ pages
  // Personal shortcuts are scoped to the staff identity, browser origin and server.
  const shortcutKey = () => 'banova.views.v1:' + JSON.stringify([state.meta.server, state.meta.user.uuid || state.meta.user.name]);
  function readShortcuts() {
    try {
      const values = JSON.parse(localStorage.getItem(shortcutKey()) || '[]');
      return Array.isArray(values) ? values.filter(v => v && typeof v.name === 'string' &&
        typeof v.to === 'string' && (/^#\/(punishments|appeals|players)(\?|$)/.test(v.to) || v.to.startsWith('rule:'))) : [];
    } catch { return []; }
  }
  function writeShortcuts(values) {
    try { localStorage.setItem(shortcutKey(),JSON.stringify(values)); return true; }
    catch { toast(t('storageFailed'),true); return false; }
  }
  function shortcutEditor(view) {
    const options = [['#/punishments?active=1',t('activeCases')],['#/players',t('players')]];
    if (can('banova.appeals')) options.push(['#/appeals',t('appeals')]);
    if (can('banova.punish')) for (const tpl of state.meta.templates) options.push(['rule:'+tpl.id,t('template')+' · '+tpl.name]);
    if (view) options.unshift([view,t('filteredView')]);
    const name = el('input',{maxlength:56,value:options[0][1]});
    const choices = el('select', {onchange:() => {name.value=choices.selectedOptions[0].textContent;}},
      options.map(([value,title])=>el('option',{value},title)));
    modal(t('newShortcut'),el('div',{},el('label',{class:'field'},t('destination'),choices),
      el('label',{class:'field'},t('shortcutName'),name)), close=>el('button',{class:'btn primary',onclick:()=>{
        const title=name.value.trim();
        if(!title) {name.focus();return;}
        const saved=readShortcuts().filter(s=>s.to!==choices.value);
        if(writeShortcuts([...saved,{name:title,to:choices.value}])) {close();route();}
      }},t('save')));
  }
  function shortcutShelf() {
    const entries=readShortcuts();
    const makeRow = (s, personal) => {
      const template = s.to.startsWith('rule:');
      if(template && (!can('banova.punish') || !state.meta.templates.some(tp=>tp.id===s.to.slice(5)))) return null;
      if(s.to.startsWith('#/appeals') && !can('banova.appeals')) return null;
      return el('div',{class:'shortcut-row'},el('button',{class:'shortcut-link',onclick:()=>template ?
        punishModal({template:s.to.slice(5)}) : go(s.to)},icon(template?'gavel':'pin'),s.name),
        personal ? el('button',{class:'shortcut-remove',title:t('deleteShortcut'),'aria-label':t('deleteShortcut')+' · '+s.name,onclick:async()=>{
          if(await verifyAction(t('deleteShortcut'),s.name) && writeShortcuts(readShortcuts().filter(v=>v.to!==s.to))) route();
        }},icon('close')) : null);
    };
    return el('section',{class:'shortcut-shelf'},el('header',{},el('h2',{},icon('pin'),t('shortcuts')),
      el('button',{class:'btn small',onclick:()=>shortcutEditor()},icon('plus'),t('newShortcut'))),
      el('div',{class:'shortcut-list'},entries.some(s=>s.to==='#/punishments?active=1') ? null : makeRow({name:t('activeCases'),to:'#/punishments?active=1'},false),
        entries.map(s=>makeRow(s,true))));
  }

  async function pageDashboard() {
    const d = await api('GET', 'dashboard');
    state.pendingAppeals = d.pendingAppeals;
    const stat = (cls, label, value, hash) => el(hash ? 'a' : 'div', { class: 'card stat ' + cls, href: hash || null },
      el('div', { class: 'label' }, label), el('div', { class: 'value' }, value));
    const lockCard = can('banova.lockdown') ? el('div', { class: 'card' }, el('h2', null, t('lockdown')),
      el('p', { class: 'muted' }, d.lockdown.active ? t('lockdownOn') + (d.lockdown.reason ? ' — ' + d.lockdown.reason : '') : t('lockdownOff')),
      d.lockdown.active
        ? el('button', { class: 'btn success', onclick: async () => { if (!await verifyAction(t('disableLockdown'),strip(state.meta.server))) return; try { await api('POST', 'lockdown', { enabled: false }); route(); } catch (e) { fail(e); } } }, t('disableLockdown'))
        : el('button', { class: 'btn danger', onclick: () => lockdownModal() }, t('enableLockdown'))) : null;
    const appealsCard = can('banova.appeals') ? el('div', { class: 'card' },
      el('h2', null, t('pendingAppeals'), el('span', { class: 'grow' }), el('a', { class: 'btn small', href: '#/appeals' }, t('viewAll'))),
      d.appeals.length ? el('div', { class: 'list' }, d.appeals.map((a) => el('a', { class: 'item', href: '#/appeal/' + a.id },
        el('div', { class: 'meta' }, a.punishment ? typeBadge(a.punishment) : null, el('strong', null, a.punishment ? a.punishment.targetName : '?'),
          el('span', { class: 'grow' }), fmtDate(a.createdAt)),
        el('p', null, a.message.length > 140 ? a.message.substring(0, 140) + '…' : a.message))))
        : el('div', { class: 'empty' }, t('noPending'))) : null;
    layout(t('dashboard'), el('div', null,
      el('div', { class: 'grid stats' },
        stat('ban', t('activeBans'), d.activeBans, '#/punishments?type=BAN&active=1'),
        stat('mute', t('activeMutes'), d.activeMutes, '#/punishments?type=MUTE&active=1'),
        stat('warn', t('activeWarns'), d.activeWarns, '#/punishments?type=WARN&active=1'),
        can('banova.appeals') ? stat('appeal', t('pendingAppeals'), d.pendingAppeals, '#/appeals') : stat('',t('online'),d.online)),
      shortcutShelf(),
      el('section',{class:'case-ledger card'},el('header',{class:'ledger-head'},el('div',{},el('h2',{},t('inbox')),
        el('small',{class:'muted'},t('today')+' · '+d.today+'   /   '+t('week')+' · '+d.week)),
        el('a',{class:'text-link',href:'#/punishments'},t('viewAll'),icon('arrow'))),punishmentTable(d.recent)),
      el('div',{class:'grid two'},appealsCard,lockCard)),punishButton());
  }

  function lockdownModal() {
    const reason = el('input', { type: 'text', placeholder: t('lockdownReason') });
    modal(t('enableLockdown'), el('label', { class: 'field' }, el('span', null, t('reason')), reason), (close) =>
      el('button', { class: 'btn danger', onclick: async () => {
        try { await api('POST', 'lockdown', { enabled: true, reason: reason.value }); close(); route(); } catch (e) { fail(e); }
      } }, t('confirm')));
  }

  async function pagePunishments(params) {
    const filters = { type: params.get('type') || '', active: params.get('active') === '1', q: params.get('q') || '', page: parseInt(params.get('page') || '1', 10) };
    const d = await api('GET', 'punishments' + q({ type: filters.type, active: filters.active ? 'true' : '', q: filters.q, page: filters.page }));
    const update = (changes) => {
      const f = Object.assign({}, filters, changes);
      go('#/punishments' + q({ type: f.type, active: f.active ? '1' : '', q: f.q, page: f.page > 1 ? f.page : '' }));
    };
    const search = el('input', { type: 'search', placeholder: t('search'), value: filters.q,
      onkeydown: (e) => { if (e.key === 'Enter') update({ q: e.target.value, page: 1 }); } });
    layout(t('punishments'), el('div', { class: 'card' },
      el('div', { class: 'filters' },
        el('select', { onchange: (e) => update({ type: e.target.value, page: 1 }) },
          el('option', { value: '' }, t('all')),
          ['BAN', 'MUTE', 'WARN', 'KICK'].map((ty) => el('option', { value: ty, selected: filters.type === ty }, t(ty.toLowerCase())))),
        el('label', { class: 'check' }, el('input', { type: 'checkbox', checked: filters.active, onchange: (e) => update({ active: e.target.checked, page: 1 }) }), t('activeOnly')),
        search, el('button',{class:'btn small',onclick:()=>shortcutEditor(location.hash)},icon('pin'),t('saveView'))),
      punishmentTable(d.items),
      pager(d.page, d.pages, (p) => update({ page: p }))), punishButton());
  }

  async function pagePunishment(id) {
    const p = await api('GET', 'punishments/' + id);
    const revokePerm = p.type === 'BAN' ? 'banova.unban' : p.type === 'MUTE' ? 'banova.unmute' : 'banova.unwarn';
    const actions = el('div', { class: 'row' },
      p.inEffect && can(revokePerm) ? el('button', { class: 'btn danger', onclick: () => revokeModal(p) }, t('revoke')) : null,
      can('banova.edit') && !p.revokedAt ? el('button', { class: 'btn', onclick: () => editModal(p) }, t('edit')) : null);
    const evidenceInput = el('input', { type: 'text', placeholder: t('evidencePlaceholder') });
    const addEvidence = async () => {
      if (!evidenceInput.value.trim()) return;
      try { showResult(await api('POST', 'punishments/' + p.id + '/evidence', { content: evidenceInput.value })); route(); } catch (e) { fail(e); }
    };
    const evidence = el('div', { class: 'card' }, el('h2', null, t('evidence')),
      p.evidence.length ? el('div', { class: 'list' }, p.evidence.map((e) => el('div', { class: 'item' },
        el('div', { class: 'meta' }, el('strong', null, '#' + e.id), e.actorName, '·', fmtDate(e.createdAt), el('span', { class: 'grow' }),
          (e.own || can('banova.evidence.delete')) ? el('button', { class: 'btn small danger', onclick: async () => {
            if (!await verifyAction(t('delete') + ' · ' + t('evidence'), p.targetName + ' · #' + e.id + '\n' + e.content)) return;
            try { showResult(await api('DELETE', 'evidence/' + e.id)); route(); } catch (err) { fail(err); }
          } }, t('delete')) : null),
        /^https?:\/\//i.test(e.content)
          ? el('p', null, el('a', { class: 'link', href: e.content, target: '_blank', rel: 'noopener noreferrer nofollow' }, e.content))
          : el('p', null, e.content)))) : el('p', { class: 'muted' }, t('noEvidence')),
      can('banova.evidence') ? el('div', { class: 'row', style: 'margin-top:12px' }, el('div', { style: 'flex:1' }, evidenceInput),
        el('button', { class: 'btn', onclick: addEvidence }, t('addEvidence'))) : null);
    const kv = (k, v) => [el('dt', null, k), el('dd', null, v)];
    layout(strip(p.typeName) + ' #' + p.id, el('div', { class: 'grid two' },
      el('div', null,
        el('div', { class: 'card' },
          el('div', { class: 'row', style: 'margin-bottom:14px' }, typeBadge(p), statusBadge(p), p.silent ? el('span', { class: 'badge' }, t('silent')) : null,
            el('span', { class: 'grow' }), actions),
          el('dl', { class: 'kv' },
            kv(t('player'), p.targetUuid ? el('a', { href: '#/player/' + p.targetUuid }, playerCell(p.targetName, p.targetUuid)) : (p.targetName || '—')),
            p.ip ? kv(t('ip'), el('span', { class: 'mono' }, p.ip)) : null,
            kv(t('reason'), p.reason),
            kv(t('staff'), p.actorName),
            kv(t('date'), fmtDate(p.createdAt)),
            p.type !== 'KICK' ? kv(t('duration'), strip(p.durationText)) : null,
            p.type !== 'KICK' ? kv(t('expires'), p.permanent ? t('permanent') : fmtDate(p.expiresAt)) : null,
            p.inEffect ? kv(t('remaining'), strip(p.remainingText)) : null,
            p.revokedAt ? kv(t('removedBy'), (p.revokedByName || '—') + ' · ' + fmtDate(p.revokedAt)) : null,
            p.revokedAt ? kv(t('removeReason'), p.revokeReason || '—') : null,
            p.template ? kv(t('template'), p.template + ' · ' + t('step') + ' ' + (p.templateStep + 1)) : null,
            p.server ? kv(t('server'), p.server) : null,
            p.appealCode ? kv(t('appealCode'), el('span', { class: 'mono' }, p.appealCode)) : null)),
        evidence),
      el('div', null, p.appeals.length ? el('div', { class: 'card' }, el('h2', null, t('appeals')),
        el('div', { class: 'list' }, p.appeals.map((a) => el('a', { class: 'item', href: '#/appeal/' + a.id },
          el('div', { class: 'meta' }, el('span', { class: 'badge ' + a.status }, strip(a.statusName)), el('span', { class: 'grow' }), fmtDate(a.createdAt)),
          el('p', null, a.message))))) : null)));
  }

  function revokeModal(p) {
    const reason = el('textarea', { placeholder: t('revokeReason') });
    modal(t('revokeTitle') + ' #' + p.id, el('label', { class: 'field' }, el('span', null, t('reason')), reason), (close) =>
      el('button', { class: 'btn danger', onclick: async () => {
        try { showResult(await api('POST', 'punishments/' + p.id + '/revoke', { reason: reason.value })); close(); route(); } catch (e) { fail(e); }
      } }, t('revoke')));
  }

  function editModal(p) {
    const reason = el('textarea', { value: p.reason });
    reason.value = p.reason;
    const duration = el('input', { type: 'text', placeholder: t('durationPlaceholder') });
    modal(t('editTitle') + ' #' + p.id, el('div', null,
      el('label', { class: 'field' }, el('span', null, t('reason')), reason),
      p.type !== 'KICK' ? el('label', { class: 'field' }, el('span', null, t('duration')), duration, el('small', { class: 'muted' }, t('editDurationHint'))) : null),
    (close) => el('button', { class: 'btn primary', onclick: async () => {
      try { showResult(await api('POST', 'punishments/' + p.id + '/edit', { reason: reason.value, duration: duration.value })); close(); route(); } catch (e) { fail(e); }
    } }, t('save')));
  }

  function punishModal(prefill) {
    const templates = (state.meta.templates || []);
    let mode = templates.length && can('banova.punish') ? 'template' : 'custom';
    const target = el('input', { type: 'text', placeholder: t('target'), value: (prefill && prefill.target) || '' });
    const typeOptions = [['BAN', ['banova.ban', 'banova.tempban']], ['MUTE', ['banova.mute', 'banova.tempmute']], ['WARN', ['banova.warn']], ['KICK', ['banova.kick']]]
      .filter(([, perms]) => perms.some(can));
    const typeSel = el('select', null, typeOptions.map(([ty]) => el('option', { value: ty }, t(ty.toLowerCase()))));
    const duration = el('input', { type: 'text', placeholder: t('durationPlaceholder') });
    const presets = el('div', { class: 'presets' }, ['30m', '1h', '6h', '1d', '3d', '7d', '30d', 'perm'].map((d) =>
      el('button', { class: 'btn small', type: 'button', onclick: () => { duration.value = d; } }, d === 'perm' ? t('permanent') : d)));
    const reason = el('input', { type: 'text', placeholder: t('reasonPlaceholder') });
    const silent = el('input', { type: 'checkbox' });
    const ip = el('input', { type: 'checkbox' });
    const templateSel = el('select', null, templates.map((tp) => el('option', { value: tp.id }, tp.name)));
    if (prefill && prefill.template) templateSel.value = prefill.template;
    const ladder = el('div', { class: 'ladder' });
    const drawLadder = () => {
      clear(ladder);
      const tp = templates.find((x) => x.id === templateSel.value);
      if (tp) tp.steps.forEach((s, i) => ladder.appendChild(el('span', { class: 'badge ' + s.type }, (i + 1) + '. ' + strip(s.typeName) + ' ' + strip(s.durationText) + (s.ip ? ' IP' : ''))));
    };
    templateSel.addEventListener('change', drawLadder);
    drawLadder();
    const templateBox = el('div', null, el('label', { class: 'field' }, el('span', null, t('template')), templateSel), ladder);
    const ipRow = el('label', { class: 'check', title: t('ipHint') }, ip, t('ipBased'));
    const customBox = el('div', null,
      el('label', { class: 'field' }, el('span', null, t('type')), typeSel),
      el('label', { class: 'field durationField' }, el('span', null, t('duration')), duration, presets),
      el('label', { class: 'field' }, el('span', null, t('reason')), reason),
      ipRow);
    const syncType = () => {
      const ty = typeSel.value;
      customBox.querySelector('.durationField').classList.toggle('hidden', ty === 'KICK');
      ipRow.classList.toggle('hidden', !((ty === 'BAN' && can('banova.ipban')) || (ty === 'MUTE' && can('banova.ipmute'))));
    };
    typeSel.addEventListener('change', syncType);
    syncType();
    const seg = el('div', { class: 'seg' });
    const draw = () => {
      clear(seg);
      if (templates.length && can('banova.punish')) {
        seg.appendChild(el('button', { type: 'button', class: mode === 'template' ? 'active' : '', onclick: () => { mode = 'template'; draw(); } }, t('template')));
      }
      if (typeOptions.length) {
        seg.appendChild(el('button', { type: 'button', class: mode === 'custom' ? 'active' : '', onclick: () => { mode = 'custom'; draw(); } }, t('custom')));
      }
      templateBox.classList.toggle('hidden', mode !== 'template');
      customBox.classList.toggle('hidden', mode !== 'custom');
    };
    draw();
    modal(t('newPunishment'), el('div', null,
      el('label', { class: 'field' }, el('span', null, t('player')), target),
      el('div', { class: 'field' }, seg),
      templateBox, customBox,
      el('div', { style: 'margin-top:10px' }, el('label', { class: 'check', title: t('silentHint') }, silent, t('silent') + ' — ' + t('silentHint')))),
    (close) => el('button', { class: 'btn primary', onclick: async (ev) => {
      const body = { target: target.value.trim(), silent: silent.checked };
      if (!body.target) { target.focus(); return; }
      if (mode === 'template') body.template = templateSel.value;
      else Object.assign(body, { type: typeSel.value, duration: duration.value.trim(), reason: reason.value, ip: ip.checked });
      const button = ev.currentTarget;
      button.disabled = true;
      try {
        const summary = body.template ? templateSel.selectedOptions[0]?.textContent :
          [t(typeSel.value.toLowerCase()), typeSel.value === 'KICK' ? '' : duration.value || t('permanent'), reason.value].filter(Boolean).join(' · ');
        if (!await verifyAction(t('newPunishment'), [target.value.trim(), summary, body.ip ? 'IP' : '', body.silent ? t('silent') : ''].filter(Boolean).join('\n'))) return;
        const res = await api('POST', 'punishments', body);
        showResult(res);
        if (res.ok) { close(); if (res.id) go('#/p/' + res.id); else route(); }
      } catch (e) { fail(e); } finally { button.disabled = false; }
    } }, t('punish')));
  }

  async function pagePlayers(params) {
    const term = params.get('q') || '';
    const d = await api('GET', 'players' + q({ q: term }));
    const input = el('input', { type: 'search', placeholder: t('searchPlayers'), value: term,
      onkeydown: (e) => { if (e.key === 'Enter') go('#/players' + q({ q: e.target.value.trim() })); } });
    layout(t('players'), el('div', { class: 'card' },
      el('div', { class: 'filters' }, input),
      term ? null : el('h2', null, t('recentPlayers')),
      d.items.length ? el('div', { class: 'table-wrap' }, el('table', null,
        el('thead', null, el('tr', null, el('th', null, t('player')), el('th', null, t('firstSeen')), el('th', null, t('lastSeen')))),
        el('tbody', null, d.items.map((p) => el('tr', { class: 'click', onclick: () => go('#/player/' + p.uuid) },
          el('td', null, playerCell(p.name, p.uuid), p.online ? el('span', { class: 'badge active', style: 'margin-left:8px' }, t('onlineNow')) : null),
          el('td', { class: 'muted' }, fmtDate(p.firstSeen)), el('td', { class: 'muted' }, p.online ? t('onlineNow') : fmtDate(p.lastSeen))))))) :
        el('div', { class: 'empty' }, t('nothing'))), punishButton());
    input.focus();
  }

  async function pagePlayer(uuid) {
    const d = await api('GET', 'players/' + encodeURIComponent(uuid));
    const pl = d.player;
    const badges = el('div', { class: 'badges' },
      pl.online ? el('span', { class: 'badge active' }, t('onlineNow')) : null,
      d.ban ? el('a', { class: 'badge BAN', href: '#/p/' + d.ban.id }, t('banned') + ' · ' + strip(d.ban.remainingText)) : null,
      d.mute ? el('a', { class: 'badge MUTE', href: '#/p/' + d.mute.id }, t('muted') + ' · ' + strip(d.mute.remainingText)) : null,
      d.warns ? el('span', { class: 'badge WARN' }, t('warnings') + ': ' + d.warns) : null);
    const head = el('div', { class: 'card' }, el('div', { class: 'profile-head' }, avatar(pl.uuid, true),
      el('div', null, el('h1', null, pl.name), badges,
        el('div', { class: 'muted', style: 'margin-top:6px;font-size:13px' },
          t('firstSeen') + ': ' + fmtDate(pl.firstSeen) + ' · ' + t('lastSeen') + ': ' + (pl.online ? t('onlineNow') : fmtDate(pl.lastSeen)) +
          (pl.ip ? ' · IP ' + pl.ip : ''))),
      el('span', { class: 'grow', style: 'flex:1' }), punishButton({ target: pl.name })));
    const templatesCard = d.templates && d.templates.length ? el('div', { class: 'card' }, el('h2', null, t('quickPunish')),
      el('div', { class: 'templates' }, d.templates.map((tp) => el('button', { class: 'template', onclick: () => {
        const silent = el('input', { type: 'checkbox' });
        modal(t('applyTemplate', { name: tp.name }), el('div', null,
          el('p', null, el('span', { class: 'badge ' + tp.type }, strip(tp.typeName) + ' ' + strip(tp.durationText)), ' → ', pl.name),
          el('label', { class: 'check' }, silent, t('silent') + ' — ' + t('silentHint'))), (close) =>
          el('button', { class: 'btn primary', onclick: async (ev) => {
            const button = ev.currentTarget;
            button.disabled = true;
            try {
              const res = await api('POST', 'punishments', { target: pl.uuid, template: tp.id, silent: silent.checked });
              showResult(res); close(); route();
            } catch (e) { fail(e); } finally { button.disabled = false; }
          } }, t('confirm')));
      } }, el('strong', null, tp.name),
      el('small', null, t('nextStep') + ': ' + strip(tp.typeName) + ' ' + strip(tp.durationText) + ' · ' + t('step') + ' ' + tp.step + '/' + tp.steps),
      el('br'), el('small', null, tp.offenses + ' ' + t('offenses')))))) : null;
    const noteInput = el('textarea', { placeholder: t('addNote') });
    const notesCard = d.notes ? el('div', { class: 'card' }, el('h2', null, t('notes')),
      d.notes.length ? el('div', { class: 'list' }, d.notes.map((n) => el('div', { class: 'item' },
        el('div', { class: 'meta' }, el('strong', null, n.actorName), fmtDate(n.createdAt), el('span', { class: 'grow' }),
          (n.own || can('banova.notes.delete')) ? el('button', { class: 'btn small danger', onclick: async () => {
            if (!await verifyAction(t('delete') + ' · ' + t('notes'), pl.name + ' · #' + n.id + '\n' + n.content)) return;
            try { showResult(await api('DELETE', 'notes/' + n.id)); route(); } catch (e) { fail(e); }
          } }, t('delete')) : null),
        el('p', null, n.content)))) : el('p', { class: 'muted' }, t('noNotes')),
      can('banova.notes.add') ? el('div', { style: 'margin-top:12px' }, noteInput, el('div', { class: 'row end', style: 'margin-top:8px' },
        el('button', { class: 'btn', onclick: async () => {
          if (!noteInput.value.trim()) return;
          try { showResult(await api('POST', 'players/' + pl.uuid + '/notes', { content: noteInput.value })); route(); } catch (e) { fail(e); }
        } }, t('add')))) : null) : null;
    const altsCard = d.alts ? el('div', { class: 'card' }, el('h2', null, t('alts')),
      d.alts.length ? el('div', { class: 'list' }, d.alts.map((a) => el('a', { class: 'item', href: '#/player/' + a.uuid },
        el('div', { class: 'meta' }, playerCell(a.name, a.uuid), el('span', { class: 'grow' }),
          a.banned ? el('span', { class: 'badge BAN' }, t('banned')) : null, a.muted ? el('span', { class: 'badge MUTE' }, t('muted')) : null,
          a.online ? el('span', { class: 'badge active' }, t('onlineNow')) : null)))) : el('p', { class: 'muted' }, t('noAlts'))) : null;
    const ipsCard = d.ips && d.ips.length ? el('div', { class: 'card' }, el('h2', null, t('ips')),
      el('table', null, el('tbody', null, d.ips.map((i) => el('tr', null, el('td', { class: 'mono' }, i.ip), el('td', { class: 'muted' }, fmtDate(i.lastSeen))))))) : null;
    layout(pl.name, el('div', null, head, templatesCard,
      el('div', { class: 'grid two', style: 'margin-top:16px' },
        el('div', { class: 'card' }, el('h2', null, t('history') + ' (' + d.historyTotal + ')'), punishmentTable(d.history, { noPlayer: true })),
        el('div', null, notesCard, altsCard, ipsCard))));
  }

  async function pageAppeals(params) {
    const status = params.get('status') || 'pending';
    const page = parseInt(params.get('page') || '1', 10);
    const d = await api('GET', 'appeals' + q({ status, page }));
    const tabs = el('div', { class: 'tabs' }, [['pending', t('pending')], ['accepted', t('accepted')], ['denied', t('denied')], ['all', t('all')]]
      .map(([s, label]) => el('button', { class: s === status ? 'active' : '', onclick: () => go('#/appeals' + q({ status: s })) }, label)));
    layout(t('appeals'), el('div', { class: 'card' }, tabs,
      d.items.length ? el('div', { class: 'list' }, d.items.map((a) => el('a', { class: 'item', href: '#/appeal/' + a.id },
        el('div', { class: 'meta' }, el('strong', null, '#' + a.id), el('span', { class: 'badge ' + a.status }, strip(a.statusName)),
          a.punishment ? typeBadge(a.punishment) : null, a.punishment ? playerCell(a.punishment.targetName, a.punishment.targetUuid) : null,
          el('span', { class: 'grow' }), fmtDate(a.createdAt)),
        el('p', null, a.message.length > 220 ? a.message.substring(0, 220) + '…' : a.message)))) : el('div', { class: 'empty' }, t('nothing')),
      pager(d.page, d.pages, (p) => go('#/appeals' + q({ status, page: p })))));
  }

  async function pageAppeal(id) {
    const a = await api('GET', 'appeals/' + id);
    const p = a.punishment;
    const response = el('textarea', { placeholder: t('response') });
    const handle = (action) => async (ev) => {
      if (!await verifyAction(t(action), '#' + a.id + ' · ' + (p?.targetName || '—') + '\n' + response.value)) return;
      ev.target.disabled = true;
      try { showResult(await api('POST', 'appeals/' + a.id + '/' + action, { response: response.value })); route(); } catch (e) { fail(e); ev.target.disabled = false; }
    };
    layout(t('appeals') + ' #' + a.id, el('div', { class: 'grid two' },
      el('div', null,
        el('div', { class: 'card' },
          el('div', { class: 'row', style: 'margin-bottom:12px' }, el('span', { class: 'badge ' + a.status }, strip(a.statusName)),
            el('span', { class: 'muted' }, t('sent') + ' ' + fmtDate(a.createdAt) + ' · ' + a.source)),
          el('h2', null, t('message')), el('p', { style: 'white-space:pre-wrap;word-break:break-word' }, a.message),
          a.contact ? el('p', { class: 'muted' }, t('contact') + ': ' + a.contact) : null,
          a.status === 'PENDING' ? el('div', { style: 'margin-top:14px' }, response,
            el('div', { class: 'row end', style: 'margin-top:10px' },
              el('button', { class: 'btn danger', onclick: handle('deny') }, t('deny')),
              el('button', { class: 'btn success', onclick: handle('accept') }, t('accept'))))
            : el('div', { class: 'item', style: 'margin-top:14px' }, el('div', { class: 'meta' }, t('handledBy') + ' ' + (a.handledByName || '—') + ' · ' + fmtDate(a.handledAt)),
              a.response ? el('p', null, a.response) : null)),
        a.previous && a.previous.length ? el('div', { class: 'card' }, el('h2', null, t('previousAppeals')),
          el('div', { class: 'list' }, a.previous.map((o) => el('div', { class: 'item' },
            el('div', { class: 'meta' }, el('span', { class: 'badge ' + o.status }, strip(o.statusName)), fmtDate(o.createdAt)),
            el('p', null, o.message), o.response ? el('p', { class: 'muted' }, '↳ ' + o.response) : null)))) : null),
      p ? el('div', { class: 'card' }, el('h2', null, t('punishment'), el('span', { class: 'grow' }), el('a', { class: 'btn small', href: '#/p/' + p.id }, t('details'))),
        el('dl', { class: 'kv' },
          el('dt', null, t('player')), el('dd', null, p.targetUuid ? el('a', { href: '#/player/' + p.targetUuid }, playerCell(p.targetName, p.targetUuid)) : p.targetName),
          el('dt', null, t('type')), el('dd', null, typeBadge(p), ' ', statusBadge(p)),
          el('dt', null, t('reason')), el('dd', null, p.reason),
          el('dt', null, t('staff')), el('dd', null, p.actorName),
          el('dt', null, t('date')), el('dd', null, fmtDate(p.createdAt)),
          el('dt', null, t('duration')), el('dd', null, strip(p.durationText))),
        el('h2', { style: 'margin-top:16px' }, t('evidence')),
        p.evidence && p.evidence.length ? el('div', { class: 'list' }, p.evidence.map((e) => el('div', { class: 'item' },
          el('div', { class: 'meta' }, e.actorName, '·', fmtDate(e.createdAt)),
          /^https?:\/\//i.test(e.content) ? el('p', null, el('a', { class: 'link', href: e.content, target: '_blank', rel: 'noopener noreferrer nofollow' }, e.content)) : el('p', null, e.content))))
          : el('p', { class: 'muted' }, t('noEvidence'))) : null));
  }

  async function pageAudit(params) {
    const actor = params.get('actor') || '';
    let page = 1;
    const body = el('tbody');
    const more = el('button', { class: 'btn', onclick: () => load() }, t('loadMore'));
    const load = async () => {
      const d = await api('GET', 'audit' + q({ actor, page }));
      d.items.forEach((e) => body.appendChild(el('tr', { class: e.punishmentId ? 'click' : '', onclick: e.punishmentId ? () => go('#/p/' + e.punishmentId) : null },
        el('td', { class: 'muted date' }, fmtDate(e.createdAt)), el('td', null, e.actorName), el('td', null, el('span', { class: 'badge' }, e.action)),
        el('td', null, e.targetName || ''), el('td', { class: 'hide-mobile' }, el('span', { class: 'reason', title: e.details || '' }, e.details || '')),
        el('td', { class: 'hide-mobile muted' }, e.source))));
      page++;
      more.classList.toggle('hidden', !d.more);
    };
    const input = el('input', { type: 'search', placeholder: t('filterStaff'), value: actor,
      onkeydown: (e) => { if (e.key === 'Enter') go('#/audit' + q({ actor: e.target.value.trim() })); } });
    layout(t('audit'), el('div', { class: 'card' }, el('div', { class: 'filters' }, input),
      el('div', { class: 'table-wrap' }, el('table', null, el('thead', null, el('tr', null, el('th', null, t('date')), el('th', null, t('staff')),
        el('th', null, t('action')), el('th', null, t('target2')), el('th', { class: 'hide-mobile' }, t('details')), el('th', { class: 'hide-mobile' }, t('source')))), body)),
      el('div', { class: 'row end', style: 'margin-top:10px' }, more)));
    await load();
  }

  async function pageSessions() {
    const d = await api('GET', 'sessions');
    layout(t('sessions'), el('div', { class: 'card' }, el('div', { class: 'table-wrap' }, el('table', null,
      el('thead', null, el('tr', null, el('th', null, t('staff')), el('th', null, t('browser')), el('th', null, t('ip')),
        el('th', null, t('lastUsed')), el('th', { class: 'hide-mobile' }, t('created')), el('th', null, ''))),
      el('tbody', null, d.items.map((s) => el('tr', null,
        el('td', null, s.name, s.current ? el('span', { class: 'badge active', style: 'margin-left:6px' }, t('current')) : null),
        el('td', null, s.browser, s.remember ? el('span', { class: 'badge', style: 'margin-left:6px' }, t('persistent')) : null),
        el('td', { class: 'mono muted' }, s.ip || '—'), el('td', { class: 'muted' }, fmtDate(s.lastUsed)),
        el('td', { class: 'hide-mobile muted' }, fmtDate(s.createdAt)),
        el('td', null, el('button', { class: 'btn small danger', onclick: async () => {
          if (!await verifyAction(t('endSession'),s.name + ' · ' + s.browser)) return;
          try { await api('DELETE', 'sessions/' + s.id); if (s.current) { state.meta = null; showLogin(); } else route(); } catch (e) { fail(e); }
        } }, t('endSession'))))))))));
  }

  // ------------------------------------------------------------------ drag and drop
  // Pointer-based so it works the same with a mouse, a pen or a finger. Items move live while dragging.
  function makeDraggable(handle, opts) {
    handle.addEventListener('pointerdown', down => {
      if (down.button !== 0 || document.body.classList.contains('dragging')) return;
      if (opts.touchHandle && down.pointerType !== 'mouse' && !down.target.closest(opts.touchHandle)) return;
      if (down.target !== handle && down.target.closest('input,select,textarea')) return;
      const sx = down.clientX, sy = down.clientY;
      let drag = null;
      const move = ev => {
        if (!drag) {
          if (Math.abs(ev.clientX - sx) + Math.abs(ev.clientY - sy) < 6) return;
          drag = begin();
          if (!drag) { stop(); return; }
        }
        ev.preventDefault();
        follow(ev.clientX, ev.clientY);
      };
      const up = () => { stop(); if (drag) end(true); };
      const cancel = () => { stop(); if (drag) end(false); };
      const key = ev => { if (ev.key === 'Escape') { ev.preventDefault(); ev.stopPropagation(); cancel(); } };
      function stop() {
        window.removeEventListener('pointermove', move);
        window.removeEventListener('pointerup', up);
        window.removeEventListener('pointercancel', cancel);
        window.removeEventListener('keydown', key, true);
      }
      window.addEventListener('pointermove', move, { passive: false });
      window.addEventListener('pointerup', up);
      window.addEventListener('pointercancel', cancel);
      window.addEventListener('keydown', key, true);

      function begin() {
        const node = opts.node();
        if (!node) return null;
        const from = node.isConnected ? { parent: node.parentNode, next: node.nextSibling } : null;
        const source = from ? node : handle;
        const r = source.getBoundingClientRect();
        const ghost = source.cloneNode(true);
        ghost.classList.add('drag-ghost');
        ghost.classList.remove('drag-placeholder', 'selected');
        ghost.setAttribute('aria-hidden', 'true');
        ghost.style.width = r.width + 'px';
        ghost.style.height = r.height + 'px';
        document.body.append(ghost);
        node.classList.add('drag-placeholder');
        document.body.classList.add('dragging');
        try { getSelection().removeAllRanges(); } catch (e) { /* ignore */ }
        return { node, from, ghost, dx: sx - r.left, dy: sy - r.top, zone: null };
      }
      function follow(x, y) {
        drag.ghost.style.transform = 'translate(' + (x - drag.dx) + 'px,' + (y - drag.dy) + 'px)';
        const under = document.elementFromPoint(x, y);
        const zone = under ? under.closest(opts.zones) : null;
        if (zone !== drag.zone) {
          if (drag.zone) drag.zone.classList.remove('drop-active');
          if (zone) zone.classList.add('drop-active');
          drag.zone = zone;
        }
        if (zone && !drag.node.contains(under)) place(zone, x, y);
        else if (!zone && !drag.from && drag.node.isConnected) drag.node.remove();
        if (y < 56) window.scrollBy(0, -14);
        else if (y > innerHeight - 56) window.scrollBy(0, 14);
      }
      function place(zone, x, y) {
        let before = null;
        for (const item of zone.querySelectorAll(':scope > ' + opts.items)) {
          if (item === drag.node) continue;
          const r = item.getBoundingClientRect();
          const ahead = opts.axis === 'y' ? y < r.top + r.height / 2 : (y < r.top || (y <= r.bottom && x < r.left + r.width / 2));
          if (ahead) { before = item; break; }
        }
        before = before || zone.querySelector(':scope > .drop-end');
        if (drag.node.parentNode !== zone || drag.node.nextSibling !== before) zone.insertBefore(drag.node, before);
      }
      function end(commit) {
        drag.ghost.remove();
        if (drag.zone) drag.zone.classList.remove('drop-active');
        drag.node.classList.remove('drag-placeholder');
        document.body.classList.remove('dragging');
        const moved = drag.from ? drag.node.parentNode !== drag.from.parent || drag.node.nextSibling !== drag.from.next : drag.node.isConnected;
        if (!commit && moved) {
          if (drag.from) drag.from.parent.insertBefore(drag.node, drag.from.next);
          else drag.node.remove();
        }
        // The click that follows a drag must not also select or add anything.
        const swallow = e => { e.stopPropagation(); e.preventDefault(); };
        handle.addEventListener('click', swallow, { capture: true, once: true });
        setTimeout(() => handle.removeEventListener('click', swallow, { capture: true }), 0);
        if (commit && moved) opts.onDrop(drag.node);
      }
    });
  }

  // ------------------------------------------------------------------ rules & settings
  const CATEGORIES = [['chat', 'catChat'], ['gameplay', 'catGameplay'], ['account', 'catAccount']];
  const STEP_TYPES = ['warn', 'mute', 'ban', 'kick'];
  const STEP_DEFAULTS = { warn: '7d', mute: '1h', ban: '1d', kick: '' };
  const DURATION_PRESETS = ['15m', '30m', '1h', '6h', '12h', '1d', '3d', '7d', '14d', '30d', '90d', 'perm'];
  const RESET_PRESETS = ['perm', '30d', '90d', '180d', '1y'];
  const MATERIALS = ['PAPER', 'BOOK', 'WRITABLE_BOOK', 'OAK_SIGN', 'NAME_TAG', 'CLOCK', 'BELL', 'TNT', 'ENDER_EYE', 'IRON_SWORD',
    'DIAMOND_SWORD', 'BOW', 'SPIDER_EYE', 'TARGET', 'FLINT_AND_STEEL', 'LAVA_BUCKET', 'BARRIER', 'ANVIL', 'REDSTONE', 'COMPASS',
    'GOLD_INGOT', 'EMERALD', 'CHEST', 'PLAYER_HEAD', 'SKELETON_SKULL', 'POISONOUS_POTATO', 'FEATHER', 'IRON_BARS', 'WITHER_ROSE'];
  const PERM_WORDS = ['perm', 'permanent', 'permanente', 'forever', 'siempre', '-1', 'never', 'nunca'];
  const UNITS = {
    en: { s: 's', m: 'min', min: 'min', h: 'h', d: ['day', 'days'], w: ['week', 'weeks'], mo: ['month', 'months'], y: ['year', 'years'] },
    es: { s: 's', m: 'min', min: 'min', h: 'h', d: ['día', 'días'], w: ['semana', 'semanas'], mo: ['mes', 'meses'], y: ['año', 'años'] }
  };
  const isPerm = (v) => PERM_WORDS.includes(String(v || '').trim().toLowerCase());
  const validDuration = (v) => {
    const s = String(v || '').trim().toLowerCase();
    return isPerm(s) || (/^(\d+\s*(mo|min|y|w|d|h|m|s))+$/.test(s) && /[1-9]/.test(s));
  };
  const sameDuration = (a, b) => (isPerm(a) && isPerm(b)) || String(a).trim().toLowerCase() === String(b).trim().toLowerCase();
  function humanDuration(v) {
    if (isPerm(v)) return t('permanent');
    const units = UNITS[lang] || UNITS.en;
    const parts = [];
    const re = /(\d+)\s*(mo|min|y|w|d|h|m|s)/g;
    let m;
    while ((m = re.exec(String(v).toLowerCase()))) {
      const n = +m[1];
      const u = units[m[2]];
      parts.push(n + ' ' + (Array.isArray(u) ? u[n === 1 ? 0 : 1] : u));
    }
    return parts.join(' ') || String(v);
  }
  const ordinal = (n) => lang === 'es' ? n + '.ª' : n + ((n % 100 >= 11 && n % 100 <= 13) ? 'th' : (['th', 'st', 'nd', 'rd'][n % 10] || 'th'));
  const slug = (s) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 48);
  const cfgField = (label, node, hint) => el('label', { class: 'rule-field' }, el('span', null, label), node, hint ? el('small', { class: 'muted' }, hint) : null);
  const datalists = () => el('div', { class: 'hidden' },
    el('datalist', { id: 'banova-durations' }, DURATION_PRESETS.map((v) => el('option', { value: v }, humanDuration(v)))),
    el('datalist', { id: 'banova-materials' }, MATERIALS.map((v) => el('option', { value: v }))));

  function configError(e) {
    if (e && e.status === 409 && e.code === 'configuration_changed') {
      toast(t('fileChanged'), true);
      leaveGuard = null;
      route();
    } else if (e && e.code === 'route_in_use') toast(t('usedByFilter'), true);
    else if (e && e.status === 400) toast(t('invalidFields'), true);
    else fail(e);
  }

  async function pageConfiguration(params) {
    const tab = params.get('tab') === 'settings' ? 'settings' : 'routes';
    const data = await api('GET', 'configuration');
    const tabs = el('div', { class: 'tabs config-tabs', role: 'tablist' }, [['routes', t('rulesTab')], ['settings', t('settingsTab')]].map(([id, label]) =>
      el('button', { type: 'button', role: 'tab', 'aria-selected': id === tab ? 'true' : 'false', class: id === tab ? 'active' : '',
        onclick: () => { if (id !== tab) go('#/configuration' + (id === 'settings' ? '?tab=settings' : '')); } }, label)));
    const body = tab === 'settings' ? settingsTab(data) : routesTab(data, params.get('route'));
    layout(t('configuration'), el('div', { class: 'config-page' }, tabs, body, datalists()));
  }

  function routesTab(data, initial) {
    const cfg = { revision: data.revision, routes: data.routes || {}, filterRoute: data.filterRoute || null };
    const lanes = {};
    const editorHost = el('div', { class: 'route-editor-host' });
    let editor = null;
    leaveGuard = () => !!(editor && editor.dirty());

    async function refresh() {
      const fresh = await api('GET', 'configuration');
      cfg.revision = fresh.revision;
      cfg.routes = fresh.routes || {};
      cfg.filterRoute = fresh.filterRoute || null;
      try { state.meta = await api('GET', 'meta'); } catch (e) { /* keep the old one */ }
    }

    let queue = Promise.resolve();
    function saveLayout(node) {
      const order = CATEGORIES.flatMap(([c]) => [...lanes[c].querySelectorAll(':scope > .route-tile:not(.add)')]
        .map((n) => ({ id: n.dataset.id, category: c })));
      counts();
      if (node && editor && editor.id === node.dataset.id) editor.setCategory(node.parentNode.dataset.cat);
      queue = queue.then(async () => {
        try {
          await api('POST', 'configuration', { kind: 'layout', revision: cfg.revision, order });
          await refresh();
          toast(t('layoutSaved'));
        } catch (e) { configError(e); }
      });
    }

    function moveWithKeys(ev, node) {
      if (ev.key === 'Enter' || ev.key === ' ') { ev.preventDefault(); select(node.dataset.id); return; }
      const zone = node.parentNode;
      const prev = node.previousElementSibling, next = node.nextElementSibling;
      if (ev.key === 'ArrowLeft' && prev && prev.matches('.route-tile:not(.add)')) zone.insertBefore(node, prev);
      else if (ev.key === 'ArrowRight' && next && next.matches('.route-tile:not(.add)')) zone.insertBefore(next, node);
      else if (ev.key === 'ArrowUp' || ev.key === 'ArrowDown') {
        const order = CATEGORIES.map(([c]) => lanes[c]);
        const target = order[order.indexOf(zone) + (ev.key === 'ArrowUp' ? -1 : 1)];
        if (!target) return;
        target.insertBefore(node, target.querySelector(':scope > .drop-end'));
      } else return;
      ev.preventDefault();
      node.focus();
      saveLayout(node);
    }

    function tile(id) {
      const r = cfg.routes[id] || {};
      const steps = Array.isArray(r.steps) ? r.steps : [];
      const node = el('div', { class: 'route-tile', 'data-id': id, tabindex: '0', role: 'button', title: r.reason || '',
        'aria-label': (r.name || id) + ' — ' + t('moveHint') },
        el('span', { class: 'grip', 'aria-hidden': 'true' }, icon('grip')),
        el('span', { class: 'tile-main' }, el('strong', null, r.name || id),
          el('span', { class: 'tile-strip', 'aria-hidden': 'true' }, steps.slice(0, 10).map((s) => el('i', { 'data-type': s.type })))),
        el('small', { class: 'tile-count', title: t('stepsCount', { n: steps.length }) }, steps.length));
      node.addEventListener('click', () => select(id));
      node.addEventListener('keydown', (ev) => { if (ev.target === node) moveWithKeys(ev, node); });
      makeDraggable(node, { node: () => node, zones: '.lane-items', items: '.route-tile:not(.add)', axis: 'grid', touchHandle: '.grip', onDrop: saveLayout });
      return node;
    }

    const board = el('section', { class: 'card route-board' },
      el('header', { class: 'board-head' }, el('h2', null, t('menuLayout')), el('p', { class: 'muted' }, t('menuLayoutHint'))),
      CATEGORIES.map(([c, key]) => {
        const items = el('div', { class: 'lane-items', 'data-cat': c });
        lanes[c] = items;
        return el('div', { class: 'lane lane-' + c },
          el('div', { class: 'lane-head' }, el('span', { class: 'lane-dot' }), el('strong', null, t(key)),
            el('span', { class: 'lane-count' }), el('small', { class: 'muted' }, t('perPage'))), items);
      }));
    for (const [id, r] of Object.entries(cfg.routes)) {
      const cat = CATEGORIES.some(([c]) => c === (r && r.category)) ? r.category : 'account';
      lanes[cat].append(tile(id));
    }
    for (const [c] of CATEGORIES) {
      lanes[c].append(el('button', { type: 'button', class: 'route-tile add drop-end', onclick: () => select(null, c) }, icon('plus'), t('newRoute')));
    }
    function counts() {
      for (const [c] of CATEGORIES) lanes[c].parentNode.querySelector('.lane-count').textContent = lanes[c].querySelectorAll(':scope > .route-tile:not(.add)').length;
    }
    counts();

    async function select(id, category) {
      if (editor && editor.id === id && id) return;
      if (editor && editor.dirty() && !(await verifyAction(t('discardTitle'), t('discardText')))) return;
      open(id, category);
    }
    function open(id, category) {
      editor = routeEditor({
        id, category, cfg,
        reopen: () => open(id, category),
        onSaved: async (savedId) => {
          leaveGuard = null;
          quietHash('#/configuration?route=' + encodeURIComponent(savedId));
          await refresh();
          await pageConfiguration(new URLSearchParams({ route: savedId }));
        },
        onDeleted: async () => {
          leaveGuard = null;
          quietHash('#/configuration');
          await refresh();
          await pageConfiguration(new URLSearchParams());
        }
      });
      clear(editorHost).append(editor.el);
      board.querySelectorAll('.route-tile.selected').forEach((n) => n.classList.remove('selected'));
      if (id) {
        const current = board.querySelector('.route-tile[data-id="' + CSS.escape(id) + '"]');
        if (current) current.classList.add('selected');
      }
      quietHash('#/configuration' + (id ? '?route=' + encodeURIComponent(id) : ''));
    }
    const first = initial && Object.hasOwn(cfg.routes, initial) ? initial : Object.keys(cfg.routes)[0];
    open(first || null, 'chat');
    return el('div', { class: 'routes-layout' }, board, editorHost);
  }

  function routeEditor({ id, category, cfg, reopen, onSaved, onDeleted }) {
    const isNew = !id;
    const rule = isNew
      ? { name: '', reason: '', category: category || 'chat', icon: 'PAPER', 'reset-after': 'perm',
          steps: [{ type: 'warn', duration: '7d' }, { type: 'mute', duration: '1h' }, { type: 'mute', duration: '1d' }] }
      : JSON.parse(JSON.stringify(cfg.routes[id] || {}));
    if (!CATEGORIES.some(([c]) => c === rule.category)) rule.category = 'account';
    const text = (value, attrs) => el('input', Object.assign({ type: 'text', value: value || '', spellcheck: 'false' }, attrs || {}));

    const name = text(rule.name, { class: 'title-input', maxlength: 100, placeholder: t('routeNamePh'), 'aria-label': t('routeName'), spellcheck: 'true' });
    const reason = el('textarea', { rows: 2, maxlength: 250, placeholder: t('routeReasonPh'), class: 'reason-input' });
    reason.value = rule.reason || '';
    const idInput = text(id, { maxlength: 64, readonly: !isNew, class: 'mono' });
    if (isNew) {
      let touched = false;
      const unique = (base) => { if (!base) return ''; let c = base, n = 2; while (Object.hasOwn(cfg.routes, c)) c = base + '-' + n++; return c; };
      idInput.addEventListener('input', () => { touched = true; });
      name.addEventListener('input', () => { if (!touched) idInput.value = unique(slug(name.value)); });
    }
    const categorySel = el('select', null, CATEGORIES.map(([c, k]) => el('option', { value: c, selected: c === rule.category }, t(k))));
    const material = text(rule.icon || 'PAPER', { list: 'banova-materials', maxlength: 80, class: 'mono' });
    const aliases = text((rule.aliases || []).join(', '), { placeholder: t('aliasesPh') });
    const permission = text(rule.permission, { maxlength: 100, placeholder: 'banova.route.' + (id || 'example'), class: 'mono' });
    const silent = el('input', { type: 'checkbox', checked: !!rule.silent });
    const aliasList = () => aliases.value.split(',').map((s) => s.trim().toLowerCase()).filter(Boolean);

    // Forget offenses after
    const resetInput = text(rule['reset-after'] || 'perm', { maxlength: 32, class: 'reset-input mono', 'aria-label': t('customValue') });
    const resetSeg = el('div', { class: 'seg wrap', role: 'group', 'aria-label': t('resetAfter') });
    const drawReset = () => {
      clear(resetSeg).append(...RESET_PRESETS.map((v) => el('button', { type: 'button', class: sameDuration(resetInput.value, v) ? 'active' : '',
        'aria-pressed': sameDuration(resetInput.value, v) ? 'true' : 'false',
        onclick: () => { resetInput.value = v; resetInput.classList.remove('invalid'); drawReset(); changed(); } },
      v === 'perm' ? t('never') : humanDuration(v))));
    };
    resetInput.addEventListener('input', () => { resetInput.classList.toggle('invalid', !validDuration(resetInput.value)); drawReset(); });

    // Escalation ladder
    const ladder = el('ol', { class: 'ladder-list', 'aria-label': t('escalation') });
    const renumber = () => [...ladder.children].forEach((c, i) => { c.querySelector('.step-n').textContent = t('offense', { n: ordinal(i + 1) }); });
    function stepCard(step) {
      const type = el('select', { class: 'step-type', 'aria-label': t('type') },
        STEP_TYPES.map((v) => el('option', { value: v, selected: v === step.type }, t(v))));
      const duration = text(step.duration || STEP_DEFAULTS[step.type] || '1h', { class: 'step-duration', list: 'banova-durations', maxlength: 32, 'aria-label': t('duration') });
      const human = el('small', { class: 'step-human' });
      const ip = el('input', { type: 'checkbox', checked: !!step.ip });
      const ipLabel = el('label', { class: 'check step-ip', title: t('ipHint') }, ip, 'IP');
      const grip = el('button', { type: 'button', class: 'grip', title: t('moveHint'), 'aria-label': t('moveHint') }, icon('grip'));
      const remove = el('button', { type: 'button', class: 'btn ghost icon step-remove', title: t('removeStep'), 'aria-label': t('removeStep') }, icon('close'));
      const card = el('li', { class: 'step-card' }, grip, el('span', { class: 'step-n' }),
        el('div', { class: 'step-fields' }, type, duration, ipLabel, human), remove);
      card.step = { type, duration, ip };
      const sync = () => {
        const v = type.value;
        card.dataset.type = v;
        duration.classList.toggle('hidden', v === 'kick');
        ipLabel.classList.toggle('hidden', v !== 'ban' && v !== 'mute');
        const ok = v === 'kick' || validDuration(duration.value);
        duration.classList.toggle('invalid', !ok);
        duration.setAttribute('aria-invalid', ok ? 'false' : 'true');
        human.textContent = v === 'kick' ? t('kickNow') : !ok ? t('badDuration')
          : v === 'warn' ? t('warnActive', { d: humanDuration(duration.value) }) : humanDuration(duration.value);
      };
      type.addEventListener('change', () => {
        if (type.value !== 'kick' && !duration.value.trim()) duration.value = STEP_DEFAULTS[type.value];
        sync();
      });
      duration.addEventListener('input', sync);
      remove.addEventListener('click', () => {
        if (ladder.children.length <= 1) { toast(t('oneStep'), true); return; }
        card.remove(); renumber(); changed();
      });
      grip.addEventListener('keydown', (ev) => {
        if (ev.key === 'ArrowUp' && card.previousElementSibling) ladder.insertBefore(card, card.previousElementSibling);
        else if (ev.key === 'ArrowDown' && card.nextElementSibling) ladder.insertBefore(card.nextElementSibling, card);
        else return;
        ev.preventDefault(); grip.focus(); renumber(); changed();
      });
      makeDraggable(grip, { node: () => card, zones: '.ladder-list', items: '.step-card', axis: 'y', onDrop: () => { renumber(); changed(); } });
      sync();
      return card;
    }
    const palette = el('div', { class: 'palette', role: 'group', 'aria-label': t('addStep') }, STEP_TYPES.map((v) => {
      const chip = el('button', { type: 'button', class: 'palette-chip', 'data-type': v, title: t('paletteHint') }, icon('plus'), t(v));
      chip.addEventListener('click', () => {
        const card = stepCard({ type: v, duration: STEP_DEFAULTS[v] });
        ladder.append(card); renumber(); changed();
        const input = card.querySelector('.step-duration:not(.hidden)');
        if (input) input.focus(); else card.querySelector('.grip').focus();
      });
      makeDraggable(chip, { node: () => stepCard({ type: v, duration: STEP_DEFAULTS[v] }), zones: '.ladder-list', items: '.step-card', axis: 'y',
        onDrop: () => { renumber(); changed(); } });
      return chip;
    }));
    (rule.steps && rule.steps.length ? rule.steps : [{ type: 'warn', duration: '7d' }]).forEach((s) => ladder.append(stepCard(s)));
    renumber();
    drawReset();

    const snapshot = () => JSON.stringify({
      name: name.value.trim(), reason: reason.value.trim(), category: categorySel.value, icon: material.value.trim().toUpperCase(),
      reset: resetInput.value.trim(), aliases: aliasList(), permission: permission.value.trim(), silent: silent.checked, id: idInput.value.trim(),
      steps: [...ladder.children].map((c) => {
        const type = c.step.type.value;
        return { type, duration: type === 'kick' ? '' : c.step.duration.value.trim(), ip: (type === 'ban' || type === 'mute') && c.step.ip.checked };
      })
    });
    let baseline = null;
    const dirty = () => baseline !== null && snapshot() !== baseline;
    const status = el('span', { class: 'save-status', role: 'status' });
    const discard = el('button', { type: 'button', class: 'btn', onclick: () => reopen() }, t('discard'));
    const saveBtn = el('button', { type: 'submit', class: 'btn primary' }, isNew ? t('createRoute') : t('saveRoute'));
    function changed() {
      if (baseline === null) return;
      const d = !isNew && snapshot() !== baseline;
      form.classList.toggle('is-dirty', d || isNew);
      status.textContent = isNew ? t('newRouteHint') : d ? t('unsaved') : t('allSaved');
      saveBtn.disabled = !d && !isNew;
      discard.disabled = !d && !isNew;
    }

    const inFilter = !isNew && cfg.filterRoute === id;
    const del = isNew ? null : el('button', { type: 'button', class: 'btn danger small', disabled: inFilter, title: inFilter ? t('usedByFilter') : null,
      onclick: async () => {
        if (!await verifyAction(t('deleteRoute') + ' · ' + (rule.name || id), t('deleteRouteText'))) return;
        try {
          await api('POST', 'configuration', { kind: 'delete', revision: cfg.revision, id });
          baseline = snapshot();
          await onDeleted();
          toast(t('routeDeleted'));
        } catch (e) { configError(e); }
      } }, t('deleteRoute'));

    const advanced = el('details', { class: 'advanced' }, el('summary', null, t('moreOptions')),
      el('div', { class: 'rule-grid' },
        cfgField(t('menuRow'), categorySel), cfgField(t('menuItem'), material),
        cfgField(t('routeId'), idInput, isNew ? t('routeIdHint') : null), cfgField(t('aliases'), aliases),
        cfgField(t('permission'), permission), el('label', { class: 'check silent-check' }, silent, t('silentRoute'))));

    const form = el('form', { class: 'card route-editor', novalidate: true },
      el('div', { class: 'editor-head' }, el('span', { class: 'section-label' }, isNew ? t('creatingRoute') : t('editingRoute')),
        el('span', { class: 'grow' }), del),
      name,
      inFilter ? el('p', { class: 'filter-note' }, t('filterNote')) : null,
      cfgField(t('routeReason'), reason),
      el('div', { class: 'escalation' },
        el('div', { class: 'escalation-head' }, el('h3', null, t('escalation')), el('p', { class: 'muted' }, t('escalationHint'))),
        palette, ladder,
        el('p', { class: 'repeat-note muted' }, icon('repeat'), t('repeatsNote'))),
      el('div', { class: 'rule-field' }, el('span', null, t('resetAfter')), el('div', { class: 'reset-row' }, resetSeg, resetInput),
        el('small', { class: 'muted' }, t('resetHint'))),
      advanced,
      el('div', { class: 'save-bar' }, status, el('span', { class: 'grow' }), discard, saveBtn));
    form.addEventListener('input', changed);
    form.addEventListener('change', changed);

    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const s = JSON.parse(snapshot());
      const bad = [];
      if (!s.name) bad.push(name);
      if (!s.reason) bad.push(reason);
      if (isNew && !/^[a-z0-9][a-z0-9_-]*$/.test(s.id)) bad.push(idInput);
      if (!validDuration(s.reset)) bad.push(resetInput);
      if (!/^[A-Z0-9_]+$/.test(s.icon)) bad.push(material);
      if (s.aliases.some((a) => /\s/.test(a) || a.length > 64)) bad.push(aliases);
      ladder.querySelectorAll('.step-duration.invalid:not(.hidden)').forEach((i) => bad.push(i));
      [name, reason, idInput, resetInput, material, aliases].forEach((i) => i.classList.toggle('invalid', bad.includes(i)));
      if (isNew && !bad.includes(idInput) && Object.hasOwn(cfg.routes, s.id)) {
        bad.push(idInput); idInput.classList.add('invalid');
        toast(t('idTaken'), true);
      } else if (bad.length) toast(t('invalidFields'), true);
      if (bad.length) {
        if ([idInput, material, aliases].some((b) => bad.includes(b))) advanced.open = true;
        bad[0].focus();
        return;
      }
      const route = Object.assign({}, isNew ? {} : cfg.routes[id], {
        name: s.name, reason: s.reason, category: s.category, icon: s.icon, 'reset-after': s.reset, silent: s.silent,
        steps: s.steps.map((st) => st.type === 'kick' ? { type: 'kick' } : { type: st.type, duration: st.duration, ip: st.ip })
      });
      if (s.aliases.length) route.aliases = s.aliases; else delete route.aliases;
      if (s.permission) route.permission = s.permission; else delete route.permission;
      saveBtn.disabled = true;
      try {
        await api('POST', 'configuration', { kind: 'route', revision: cfg.revision, id: isNew ? s.id : id, route });
        baseline = snapshot();
        await onSaved(isNew ? s.id : id);
        toast(t('routeSaved'));
      } catch (e) { configError(e); changed(); }
    });

    baseline = snapshot();
    changed();
    return {
      el: form, id, dirty,
      setCategory: (c) => {
        categorySel.value = c;
        const b = JSON.parse(baseline); b.category = c; baseline = JSON.stringify(b);
        changed();
      }
    };
  }

  function settingsTab(data) {
    const s = data.settings || {};
    const choices = (name, value, options) => el('div', { class: 'choices', role: 'radiogroup' }, options.map(([v, title, hint]) =>
      el('label', { class: 'choice' }, el('input', { type: 'radio', name, value: v, checked: v === value }),
        el('span', { class: 'choice-body' }, el('strong', null, title), hint ? el('small', null, hint) : null))));
    const toggle = (name, checked, title, hint) => el('label', { class: 'switch-row' },
      el('span', null, el('strong', null, title), el('small', { class: 'muted' }, hint)),
      el('input', { type: 'checkbox', role: 'switch', class: 'switch', name, checked: !!checked }));
    const status = el('span', { class: 'save-status', role: 'status' });
    const saveBtn = el('button', { type: 'submit', class: 'btn primary' }, t('saveSettings'));
    const discard = el('button', { type: 'button', class: 'btn', onclick: () => pageConfiguration(new URLSearchParams({ tab: 'settings' })) }, t('discard'));
    const form = el('form', { class: 'settings-form', novalidate: true },
      el('section', { class: 'card setting-card' }, el('h2', null, t('langSetting')), el('p', { class: 'muted' }, t('langSettingHint')),
        choices('language', s.language === 'es' ? 'es' : 'en', [['en', 'English', '“You have been banned”'], ['es', 'Español', '“Has sido baneado”']]),
        el('p', { class: 'note muted' }, t('langNote'))),
      el('section', { class: 'card setting-card' }, el('h2', null, t('broadcastSetting')), el('p', { class: 'muted' }, t('broadcastHint')),
        choices('broadcast', s.broadcast || 'all', [['all', t('bcAll'), t('bcAllHint')], ['staff', t('bcStaff'), t('bcStaffHint')], ['none', t('bcNone'), t('bcNoneHint')]])),
      el('section', { class: 'card setting-card' }, el('h2', null, t('behaviour')),
        toggle('silentByDefault', s.silentByDefault, t('silentDefault'), t('silentDefaultHint')),
        toggle('alertBanEvasion', s.alertBanEvasion, t('evasion'), t('evasionHint')),
        toggle('appealsEnabled', s.appealsEnabled, t('appealsSetting'), t('appealsSettingHint'))),
      el('div', { class: 'save-bar' }, status, el('span', { class: 'grow' }), discard, saveBtn));
    const read = () => ({
      language: form.elements.language.value, broadcast: form.elements.broadcast.value,
      silentByDefault: form.elements.silentByDefault.checked, alertBanEvasion: form.elements.alertBanEvasion.checked,
      appealsEnabled: form.elements.appealsEnabled.checked
    });
    const baseline = JSON.stringify(read());
    const dirty = () => JSON.stringify(read()) !== baseline;
    const changed = () => {
      const d = dirty();
      form.classList.toggle('is-dirty', d);
      status.textContent = d ? t('unsaved') : t('allSaved');
      saveBtn.disabled = !d;
      discard.disabled = !d;
    };
    form.addEventListener('change', changed);
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const values = read();
      saveBtn.disabled = true;
      try {
        await api('POST', 'configuration', { kind: 'settings', revision: data.settingsRevision, settings: values });
        try { state.meta = await api('GET', 'meta'); } catch (e) { /* keep the old one */ }
        if (!savedLang()) { lang = values.language; document.documentElement.lang = lang; }
        leaveGuard = null;
        await pageConfiguration(new URLSearchParams({ tab: 'settings' }));
        toast(t('settingsSaved'));
      } catch (e) { configError(e); changed(); }
    });
    leaveGuard = dirty;
    changed();
    return form;
  }

  // ------------------------------------------------------------------ router
  async function route() {
    if (!state.meta) return;
    const hash = location.hash || '#/';
    const [path, query] = hash.substring(1).split('?');
    const params = new URLSearchParams(query || '');
    const parts = path.split('/').filter(Boolean);
    leaveGuard = null;
    currentHash = location.hash;
    try {
      if (parts.length === 0) {
        if (can('banova.history')) await pageDashboard();
        else if (can('banova.appeals')) go('#/appeals');
        else await pageSessions();
      } else if (parts[0] === 'punishments') await pagePunishments(params);
      else if (parts[0] === 'p' && parts[1]) await pagePunishment(parts[1]);
      else if (parts[0] === 'players') await pagePlayers(params);
      else if (parts[0] === 'player' && parts[1]) await pagePlayer(parts[1]);
      else if (parts[0] === 'appeals' && parts[1]) go('#/appeal/' + parts[1]);
      else if (parts[0] === 'appeals') await pageAppeals(params);
      else if (parts[0] === 'appeal' && parts[1]) await pageAppeal(parts[1]);
      else if (parts[0] === 'audit') await pageAudit(params);
      else if (parts[0] === 'configuration') await pageConfiguration(params);
      else if (parts[0] === 'sessions') await pageSessions();
      else go('#/');
      window.scrollTo(0, 0);
    } catch (e) {
      if (e && e.status === 401) return;
      fail(e);
      if (e && e.status === 404) go('#/');
    }
  }

  async function boot() {
    const hash = location.hash || '';
    if (hash.startsWith('#/link/')) {
      const token = hash.substring('#/link/'.length);
      history.replaceState(null, '', location.pathname + '#/');
      try { await api('POST', 'auth/link', { token, remember: true }); } catch (e) { toast(t('linkInvalid'), true); }
    }
    try {
      state.meta = await api('GET', 'meta');
    } catch (e) {
      if (e.status === 401 || e.status === 403) {
        if (!savedLang()) {
          try { lang = (await api('GET', 'public/info')).language === 'es' ? 'es' : 'en'; } catch (err) { /* stay in English */ }
          document.documentElement.lang = lang;
        }
        showLogin();
        return;
      }
      clear($('#app')).appendChild(el('div', { class: 'boot' }, t('error')));
      return;
    }
    if (!savedLang() && state.meta.language) lang = state.meta.language === 'es' ? 'es' : 'en';
    document.documentElement.lang = lang;
    if (can('banova.appeals')) {
      try { state.pendingAppeals = (await api('GET', 'appeals?status=pending')).total; } catch (e) { /* ignore */ }
    }
    route();
  }

  window.addEventListener('hashchange', async () => {
    if (leaveGuard && leaveGuard()) {
      const target = location.hash;
      history.replaceState(null, '', currentHash || '#/');
      if (!await verifyAction(t('discardTitle'), t('discardText'))) return;
      leaveGuard = null;
      location.hash = target;
      return;
    }
    route();
  });
  window.addEventListener('beforeunload', (e) => {
    if (leaveGuard && leaveGuard()) { e.preventDefault(); e.returnValue = ''; }
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') clear($('#modal-root')); });
  boot();
})();
