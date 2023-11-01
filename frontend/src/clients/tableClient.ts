import {Room, RoomId} from "../model/room";
import {GameClient, GameClientBuilder} from "./gameClient";
import {PlayerState, UserId} from "../model/player";

class Game {
    room?: Room
    client: GameClient
    onRoomUpdate: (room: Room, client: GameClient) => void

    updateRoom(f: (room: Room) => Room): void {
        if (this.room) {
            this.room = f(this.room)
            this.onRoomUpdate(this.room, this.client)
        }
    }

    constructor(authToken: string, roomId: RoomId, onRoomUpdate: (room: Room, client: GameClient) => void) {
        this.onRoomUpdate = onRoomUpdate
        this.client = new GameClientBuilder()
            .onReady(() => {
                console.debug('Ready')
                this.client.authenticate(authToken)
            })
            .onAuthenticated((user) => {
                console.debug(`Authenticated as ${user.id}`)
                this.client.connect()
            })
            .onConnected((room) => {
                console.debug('Connected')
                this.room = room
                this.client.joinTable()
            })
            .onPlayerJoinedTable((event) => {
                console.debug(`${event.user.name} joined the table`)
                this.updateRoom((room) => {
                    return {
                        seats: room.seats.map((seat) => {
                            if (seat.index === event.seat) {
                                return {
                                    occupant: event.user,
                                    ...seat
                                }
                            } else {
                                return seat
                            }
                        }),
                        players: {
                            [event.user.id]: event.user,
                            ...room.players
                        },
                        ...room
                    }
                })
            })
            .onPlayerLeftTable((event) => {
                console.log(`${event.user.name} left the table`)
                this.updateRoom((room) => {
                    return {
                        seats: room.seats.map((seat) => {
                            if (seat.index === event.seat) {
                                return {
                                    occupant: undefined,
                                    ...seat
                                }
                            } else {
                                return seat
                            }
                        }),
                        ...room
                    }
                })
            })
            .onMatchStarted((event) => {
                console.debug(`${event.gameType} match started`)
                this.updateRoom((room) => {
                    const matchPlayerIds: UserId[] = event.matchScores
                        .flatMap((score) => score.playerIds)

                    return {
                        seats: room.seats.map((seat) => {
                            if (seat.occupant && matchPlayerIds.includes(seat.occupant.player.id)) {
                                return {
                                    occupant: {
                                        state: PlayerState.Playing,
                                        player: {
                                            id: seat.occupant.player.id,
                                            name: seat.occupant.player.name,
                                            points: 0,
                                        }
                                    },
                                    hand: [],
                                    pile: []
                                }
                            } else if (seat.occupant) {
                                return {
                                    occupant: {
                                        state: PlayerState.SittingOut,
                                        player: {
                                            id: seat.occupant.player.id,
                                            name: seat.occupant.player.name,
                                        }
                                    },
                                    hand: [],
                                    pile: []
                                }
                            } else {
                                return {
                                    occupant: undefined,
                                    hand: [],
                                    pile: []
                                }
                            }
                        }),
                        matchInfo: {
                            gameType: event.gameType,
                            matchScore: event.matchScores,
                            gameScore: undefined
                        },
                        ...room
                    }
                })
            })
            .onTrumpRevealed((event) => {
                console.debug(`Trump revealed: ${event.card}`)
                this.updateRoom((room) => {
                    return {
                        deck: room.deck.filter((card) => card.ref !== event.card.ref).concat([event.card]),
                        ...room
                    }
                })
            })
            .onBoardCardsDealt((event) => {
                console.debug('Board cards dealt')
                this.updateRoom((room) => {
                    return {
                        board: room.board.concat(event.cards.map((card) => {
                            return {
                                card,
                                playedBy: undefined
                            }
                        })),
                        ...room
                    }
                })
            })
            .onCardPlayed((event) => {
                console.debug(`Card played by ${event.playerId} ${event.card}`)
                this.updateRoom((room) => {
                    return {
                        seats: room.seats.map((seat) => {
                            if (seat.occupant && seat.occupant.state == PlayerState.Acting) {
                                return {
                                    occupant: {
                                        state: PlayerState.Waiting,
                                        player: seat.occupant.player
                                    },
                                    hand: seat.hand.filter((card) => card.ref !== event.card.ref),
                                    ...seat
                                }
                            }
                            else {
                                return seat
                            }
                        }),
                        board: [
                            {
                                card: event.card,
                                playedBy: event.playerId
                            },
                            ...room.board
                        ],
                        ...room
                    }
                })
            })
            .onCardsTaken((event, gameClient) => {
                console.debug(`Cards taken by ${event.playerId}: plays ${event.card}, takes ${event.taken}`)
                this.updateRoom((room) => {
                    return {
                        seats: room.seats.map((seat) => {
                            if (seat.occupant && seat.occupant.state == PlayerState.Acting) {
                                return {
                                    occupant: {
                                        state: PlayerState.Waiting,
                                        player: seat.occupant.player
                                    },
                                    hand: seat.hand.filter((card) => card.ref !== event.card.ref),
                                    pile: seat.pile.concat(event.taken.map(card => {
                                        return {ref: card.ref}
                                    }))
                                }
                            }
                            else {
                                return seat
                            }
                        }),
                        board: room.board.filter((card) => !event.taken.map(card => card.ref).includes(card.card.ref)),
                        ...room
                    }
                })
            })
            .onPlayerConfirmed((event) => {
                console.debug(`Player ${event.playerId} confirmed`)
                this.updateRoom((room) => {
                    return {
                        seats: room.seats.map((seat) => {
                            if (seat.occupant && seat.occupant.state == PlayerState.Acting) {
                                return {
                                    occupant: {
                                        state: PlayerState.Waiting,
                                        player: seat.occupant.player
                                    },
                                    ...seat
                                }
                            }
                            else {
                                return seat
                            }
                        }),
                        ...room
                    }
                })
            })
            .build(roomId)
    }
}
