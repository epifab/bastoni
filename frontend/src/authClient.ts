export function fetchAuthToken(userName: string, host: string = 'localhost:9090', secure: boolean = false) {
    const baseUrl = `${secure ? 'https' : 'http'}://${host}/auth`;
    const headers = new Headers({"x-user-name": userName});
    return fetch(baseUrl, {method: 'POST', headers})
        .then(response => response.json())
        .then(json => json.authToken)
}
