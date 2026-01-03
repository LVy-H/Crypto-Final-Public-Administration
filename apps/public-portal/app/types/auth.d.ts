// Extend nuxt-auth-utils User type
declare module '#auth-utils' {
    interface User {
        username: string
        email?: string
        role: 'CITIZEN' | 'ADMIN' | 'OFFICER'
        fullName?: string
        verified?: boolean
    }

    interface UserSession {
        loggedInAt?: Date
        sessionId?: string
    }
}

export { }
