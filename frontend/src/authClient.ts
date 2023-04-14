export class AuthClient {
    baseUrl: string

    constructor(host: string = 'localhost:9090', secure: boolean = false) {
        this.baseUrl = `${secure ? 'https' : 'http'}://${host}/auth`
    }

    connect(userName: string): Promise<Response> {
        const headers = new Headers();
        headers.set('x-user-name', userName);

        return fetch(this.baseUrl, {method: 'POST', headers});
    }
}
