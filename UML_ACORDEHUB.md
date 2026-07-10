# UML AcordeHub

Este documento resume la arquitectura UML de AcordeHub para la version web y mobile, basado en la estructura actual del proyecto.

## 1. Vista general del sistema

```mermaid
flowchart LR
    UsuarioWeb[Usuario Web] --> WebApp[Next.js Web App]
    UsuarioMobile[Usuario Mobile] --> AndroidApp[Android App Java]

    WebApp --> FirebaseAuth[Firebase Auth]
    WebApp --> Firestore[(Cloud Firestore)]
    WebApp --> Storage[(Firebase Storage)]

    AndroidApp --> FirebaseAuth
    AndroidApp --> Firestore
    AndroidApp --> Storage
    AndroidApp --> SpotifyAPI[Spotify API]

    Firestore --> Users[(users)]
    Firestore --> Projects[(projects)]
    Firestore --> Chats[(chats)]
    Firestore --> UserLikes[(userLikes)]
    Firestore --> Matches[(matches)]
```

## 2. UML de componentes - Web

```mermaid
flowchart TB
    Layout[RootLayout] --> AuthProvider[AuthProvider]
    Layout --> Navbar[Navbar]
    AuthProvider --> UseAuth[useAuth]

    PageHome[/app/page.tsx/] --> HomeDashboard[HomeDashboard]
    PageSearch[/app/search/page.tsx/] --> SearchPage[SearchPage]
    PageProjects[/app/projects/page.tsx/] --> ProjectsPage[ProjectsPage]
    PageProfile[/app/profile/page.tsx/] --> ProfilePage[ProfilePage]
    PageLogin[/app/login/page.tsx/] --> AuthFormLogin[AuthForm mode login]
    PageRegister[/app/register/page.tsx/] --> AuthFormRegister[AuthForm mode register]

    HomeDashboard --> UseAuth
    SearchPage --> UseAuth
    ProjectsPage --> UseAuth
    ProfilePage --> UseAuth
    Navbar --> UseAuth
    AuthFormLogin --> FirebaseWeb[lib/firebase.ts]
    AuthFormRegister --> FirebaseWeb

    HomeDashboard --> Firestore[(Firestore)]
    SearchPage --> Firestore
    ProjectsPage --> Firestore
    ProfilePage --> Firestore
    ProjectsPage --> Storage[(Firebase Storage)]
    AuthProvider --> Auth[(Firebase Auth)]
    AuthFormLogin --> Auth
    AuthFormRegister --> Auth
```

## 3. UML de clases - Web

```mermaid
classDiagram
    class AuthProvider {
        -User user
        -boolean loading
        +useAuth() AuthContextValue
    }

    class AuthForm {
        -mode login|register
        -formData
        -error string
        -loading boolean
        +handleSubmit(event)
        +handleGoogle()
        -saveInitialUser(uid, email, name)
    }

    class HomeDashboard {
        -UserProfile profile
        -Project[] projects
        -UserProfile[] musicians
        -number activeProjectsCount
        +loadRealtimeDashboard()
    }

    class ProjectsPage {
        -Project[] projects
        -boolean showForm
        -File cover
        -File demo
        +publishProject(event)
        -uploadProjectFile(uid, projectId, kind, file)
    }

    class ProfilePage {
        -UserProfile profile
        -UserProfile draft
        -boolean isEditing
        +save()
        +cancel()
        +toggleArrayValue(field, value)
    }

    class SearchPage {
        -string queryText
        -string activeChip
        -UserProfile[] musicians
        +filteredMusicians()
    }

    class UserProfile {
        +string uid
        +string name
        +string email
        +string photoUrl
        +boolean isPremium
        +string role
        +string[] genres
        +string[] instruments
        +string level
        +string description
        +string location
        +FavoriteArtist[] favoriteArtists
        +Timestamp createdAt
    }

    class FavoriteArtist {
        +string id
        +string name
        +string imageUrl
    }

    class Project {
        +string id
        +string ownerUid
        +string ownerName
        +string title
        +string description
        +string genre
        +string imageUri
        +string demoUri
        +string status
        +Timestamp createdAt
    }

    AuthProvider --> AuthForm : provee estado de sesion
    HomeDashboard --> UserProfile
    HomeDashboard --> Project
    ProjectsPage --> Project
    ProfilePage --> UserProfile
    SearchPage --> UserProfile
    UserProfile "1" o-- "*" FavoriteArtist
```

## 4. Flujo web - registro/login

```mermaid
sequenceDiagram
    actor Usuario
    participant AuthForm
    participant FirebaseAuth as Firebase Auth
    participant Firestore
    participant Router as Next Router

    Usuario->>AuthForm: completa formulario
    AuthForm->>FirebaseAuth: signIn/createUser/signInWithPopup
    FirebaseAuth-->>AuthForm: credencial de usuario
    AuthForm->>Firestore: getDoc users/{uid}
    alt Perfil no existe
        AuthForm->>Firestore: setDoc users/{uid} con defaultProfile()
    end
    AuthForm->>Router: push("/")
```

## 5. Flujo web - publicar proyecto

```mermaid
sequenceDiagram
    actor Usuario
    participant ProjectsPage
    participant Firestore
    participant Storage as Firebase Storage

    Usuario->>ProjectsPage: carga titulo, descripcion, genero y demo
    ProjectsPage->>Firestore: getDoc users/{uid}
    Firestore-->>ProjectsPage: perfil del creador
    ProjectsPage->>Firestore: addDoc projects
    Firestore-->>ProjectsPage: projectId
    opt portada
        ProjectsPage->>Storage: uploadBytes cover
        Storage-->>ProjectsPage: imageUri
    end
    ProjectsPage->>Storage: uploadBytes demo
    Storage-->>ProjectsPage: demoUri
    ProjectsPage->>Firestore: addDoc projects/{id}/activity
    ProjectsPage->>Firestore: updateDoc projects/{id}
```

## 6. UML de componentes - Mobile

```mermaid
flowchart TB
    LoginActivity[LoginActivity] --> AuthVM[AuthVistaModelo]
    RegisterActivity[RegisterActivity] --> AuthVM
    MainActivity[MainActivity] --> NavGraph[Navigation Component]
    NavGraph --> HomeFragment[HomeFragment]
    NavGraph --> ChatFragment[ChatFragment]
    NavGraph --> PerfilFragment[PerfilFragment]

    HomeFragment --> PerfilVM[PerfilVistaModelo]
    HomeFragment --> ProjectVM[ProjectVistaModelo]
    PerfilFragment --> PerfilVM
    PerfilFragment --> AuthVM
    ChatFragment --> ChatVM[ChatVistaModelo]
    ChatDetailActivity[ChatDetailActivity] --> ChatVM

    PublishProjectActivity[PublishProjectActivity] --> Firestore[(Firestore)]
    PublishProjectActivity --> Storage[(Firebase Storage)]
    ProjectDetailActivity[ProjectDetailActivity] --> Firestore
    ExploreMatchActivity[ExploreMatchActivity] --> Firestore
    SpotifyArtistSearchActivity[SpotifyArtistSearchActivity] --> SpotifyService[SpotifyService Retrofit]

    AuthVM --> AuthRepo[AuthRepository]
    PerfilVM --> PerfilRepo[PerfilRepository]
    ProjectVM --> ProjectRepo[ProjectRepository]
    ChatVM --> ChatRepo[ChatRepository]

    AuthRepo --> FirebaseAuth[Firebase Auth]
    AuthRepo --> Firestore
    PerfilRepo --> FirebaseAuth
    PerfilRepo --> Firestore
    PerfilRepo --> Storage
    ProjectRepo --> FirebaseAuth
    ProjectRepo --> Firestore
    ChatRepo --> FirebaseAuth
    ChatRepo --> Firestore
    SpotifyService --> SpotifyAPI[Spotify API]
```

## 7. UML de clases - Mobile

```mermaid
classDiagram
    class AuthVistaModelo {
        -AuthRepository repository
        -LiveData userLiveData
        -LiveData errorLiveData
        -LiveData loadingLiveData
        +register(name, email, password)
        +login(email, password)
        +loginWithGoogle(account)
        +logout()
        +getCurrentUser()
    }

    class AuthRepository {
        -FirebaseAuth firebaseAuth
        -FirebaseFirestore db
        +registerWithEmail(name, email, password, callback)
        +loginWithEmail(email, password, callback)
        +loginWithGoogle(account, callback)
        +logout()
        +getCurrentUser()
        -saveUserToFirestore(uid, name, email, callback)
    }

    class PerfilVistaModelo {
        -PerfilRepository repository
        -LiveData perfilLiveData
        -LiveData errorLiveData
        -LiveData loadingLiveData
        +cargarPerfil()
        +guardarPerfil(name, role, genres, instruments, level, description, location, photoUrl, favoriteArtists)
        +subirFoto(imageUri)
    }

    class PerfilRepository {
        -FirebaseFirestore db
        -FirebaseStorage storage
        -FirebaseAuth auth
        +getPerfil(callback)
        +guardarPerfil(user, callback)
        +subirFoto(imageUri, callback)
        -createDefaultUser()
    }

    class ProjectVistaModelo {
        -ProjectRepository repository
        -LiveData projectsLiveData
        -LiveData joinRequestsLiveData
        -LiveData activeProjectsCountLiveData
        +escucharProyectosDestacados()
        +escucharMisProyectosActivos()
        +escucharSolicitudesRecibidas()
        +responderSolicitud(request, status, callback)
    }

    class ProjectRepository {
        -FirebaseAuth auth
        -FirebaseFirestore db
        +listenToFeaturedProjects(callback)
        +listenToMyActiveProjectsCount(callback)
        +listenToReceivedJoinRequests(callback)
        +respondToJoinRequest(request, status, callback)
    }

    class ChatVistaModelo {
        -ChatRepository repository
        -LiveData conversationsLiveData
        -LiveData messagesLiveData
        -LiveData openedChatLiveData
        +escucharConversaciones()
        +escucharMensajes(chatId)
        +iniciarChatConEmail(email)
        +enviarMensaje(chatId, text)
        +getCurrentUid()
    }

    class ChatRepository {
        -FirebaseAuth auth
        -FirebaseFirestore db
        +listenToConversations(callback)
        +listenToMessages(chatId, callback)
        +startChatWithEmail(email, callback)
        +startChatWithUid(otherUid, callback)
        +sendMessage(chatId, text, callback)
        -createOrOpenChat(currentUid, firebaseUser, otherUser, callback)
        -buildChatId(firstUid, secondUid)
    }

    class SpotifyService {
        +getToken(basicAuth, grantType)
        +searchArtists(token, query, type, market)
    }

    AuthVistaModelo --> AuthRepository
    PerfilVistaModelo --> PerfilRepository
    ProjectVistaModelo --> ProjectRepository
    ChatVistaModelo --> ChatRepository
    SpotifyArtistSearchActivity --> SpotifyService
```

## 8. UML de modelos - Mobile y Firebase

```mermaid
classDiagram
    class UserModel {
        +string uid
        +string name
        +string email
        +string photoUrl
        +boolean isPremium
        +string role
        +List~String~ genres
        +List~String~ instruments
        +string level
        +string description
        +string location
        +List~FavoriteArtist~ favoriteArtists
        +Date createdAt
    }

    class FavoriteArtist {
        +string name
        +string imageUrl
    }

    class Project {
        +string id
        +string ownerUid
        +string ownerName
        +string title
        +string description
        +string genre
        +string imageUri
        +string demoUri
        +string status
        +Date createdAt
        +isActive()
        +hasDemo()
    }

    class ProjectJoinRequest {
        +string id
        +string projectId
        +string projectTitle
        +string ownerUid
        +string requesterUid
        +string requesterName
        +string requesterEmail
        +string status
        +Date createdAt
        +isPending()
    }

    class ChatConversation {
        +string id
        +List~String~ participantIds
        +Map~String,String~ participantNames
        +Map~String,String~ participantEmails
        +Map~String,String~ participantPhotos
        +string lastMessage
        +Date createdAt
        +Date updatedAt
        +getOtherParticipantId(currentUid)
        +getDisplayName(currentUid)
        +getDisplayPhoto(currentUid)
    }

    class ChatMessage {
        +string id
        +string senderId
        +string text
        +Date createdAt
    }

    class MatchCandidate {
        -UserModel user
        -int score
        -string reason
        +getUser()
        +getScore()
        +getReason()
        -calculateScore(currentUser, otherUser)
        -buildReason(currentUser, otherUser)
    }

    UserModel "1" o-- "*" FavoriteArtist
    Project "1" o-- "*" ProjectJoinRequest
    ChatConversation "1" o-- "*" ChatMessage
    MatchCandidate --> UserModel
```

## 9. Navegacion mobile

```mermaid
stateDiagram-v2
    [*] --> LoginActivity
    LoginActivity --> RegisterActivity: crear cuenta
    RegisterActivity --> MainActivity: registro correcto
    LoginActivity --> MainActivity: login correcto

    MainActivity --> HomeFragment: startDestination
    MainActivity --> PerfilFragment: bottom nav
    MainActivity --> ChatFragment: bottom nav

    HomeFragment --> NotificationsActivity: notificaciones
    HomeFragment --> ChatFragment: mensajes
    HomeFragment --> PublishProjectActivity: crear proyecto
    HomeFragment --> ExploreMatchActivity: explorar
    HomeFragment --> ProjectDetailActivity: ver proyecto

    PerfilFragment --> SpotifyArtistSearchActivity: vincular artistas
    PerfilFragment --> PremiumActivity: premium
    PerfilFragment --> LoginActivity: logout

    ChatFragment --> ChatDetailActivity: abrir conversacion
    NotificationsActivity --> ChatDetailActivity: abrir chat
    ExploreMatchActivity --> ChatDetailActivity: match/chat
```

## 10. Flujo mobile - chat

```mermaid
sequenceDiagram
    actor Usuario
    participant ChatFragment
    participant ChatVM as ChatVistaModelo
    participant ChatRepo as ChatRepository
    participant Firestore
    participant ChatDetail as ChatDetailActivity

    Usuario->>ChatFragment: ingresa email o abre conversacion
    ChatFragment->>ChatVM: iniciarChatConEmail(email)
    ChatVM->>ChatRepo: startChatWithEmail(email)
    ChatRepo->>Firestore: query users where email
    Firestore-->>ChatRepo: UserModel destino
    ChatRepo->>Firestore: set chats/{uid_uid}
    ChatRepo-->>ChatVM: chatId, title
    ChatVM-->>ChatFragment: openedChatLiveData
    ChatFragment->>ChatDetail: startActivity(chatId)
    ChatDetail->>ChatVM: escucharMensajes(chatId)
    ChatVM->>ChatRepo: listenToMessages(chatId)
    ChatRepo->>Firestore: addSnapshotListener chats/{id}/messages
    Firestore-->>ChatDetail: mensajes en tiempo real
```

## 11. Flujo mobile - match

```mermaid
sequenceDiagram
    actor Usuario
    participant ExploreMatchActivity
    participant Firestore
    participant MatchCandidate
    participant ChatDetailActivity

    Usuario->>ExploreMatchActivity: abre explorar
    ExploreMatchActivity->>Firestore: get users/{uid}
    Firestore-->>ExploreMatchActivity: usuario actual
    ExploreMatchActivity->>Firestore: get users
    Firestore-->>ExploreMatchActivity: candidatos
    ExploreMatchActivity->>MatchCandidate: calcular score y razon
    MatchCandidate-->>ExploreMatchActivity: candidatos ordenados
    Usuario->>ExploreMatchActivity: like a usuario
    ExploreMatchActivity->>Firestore: set userLikes/{uid}/likedUsers/{otherUid}
    ExploreMatchActivity->>Firestore: consulta like reciproco
    alt hay match
        ExploreMatchActivity->>Firestore: set matches/{matchId}
        ExploreMatchActivity->>ChatDetailActivity: abrir chat
    end
```

## 12. Colecciones Firebase usadas

```mermaid
erDiagram
    users ||--o{ projects : "ownerUid"
    users ||--o{ chats : "participantIds"
    users ||--o{ userLikes : "likes"
    projects ||--o{ joinRequests : "solicitudes"
    projects ||--o{ activity : "eventos"
    chats ||--o{ messages : "mensajes"

    users {
        string uid
        string name
        string email
        string photoUrl
        boolean isPremium
        string role
        array genres
        array instruments
        string level
        string description
        string location
        array favoriteArtists
        timestamp createdAt
    }

    projects {
        string ownerUid
        string ownerName
        string title
        string description
        string genre
        string imageUri
        string demoUri
        string status
        timestamp createdAt
    }

    chats {
        array participantIds
        map participantNames
        map participantEmails
        map participantPhotos
        string lastMessage
        timestamp createdAt
        timestamp updatedAt
    }

    messages {
        string senderId
        string text
        timestamp createdAt
    }

    joinRequests {
        string projectId
        string projectTitle
        string ownerUid
        string requesterUid
        string requesterName
        string requesterEmail
        string status
        timestamp createdAt
    }
```
