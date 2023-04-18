export class AuthClient {
    baseUrl: string

    constructor(host: string = 'localhost:9090', secure: boolean = false) {
        this.baseUrl = `${secure ? 'https' : 'http'}://${host}/auth`
    }

    connect(userName: string): Promise<string> {
        const headers = new Headers();
        headers.set('x-user-name', userName);

        return fetch(this.baseUrl, {method: 'POST', headers})
            .then(response => response.json())
            .then(json => json.authToken);
    }
}
