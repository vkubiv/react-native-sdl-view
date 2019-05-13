#include "SDL.h"


int SDL_main(int argc, char* argv[]) {

    if (SDL_Init(SDL_INIT_VIDEO) != 0) {
        SDL_Log("Failed to initialize SDL: %s", SDL_GetError());
        return 1;
    }

    SDL_Window *sdlWindow = SDL_CreateWindow("My Game Window",
                              SDL_WINDOWPOS_UNDEFINED,
                              SDL_WINDOWPOS_UNDEFINED,
                              0, 0, SDL_WINDOW_FULLSCREEN_DESKTOP);

    SDL_Renderer *sdlRenderer = SDL_CreateRenderer(sdlWindow, -1, 0);

    SDL_Surface * image = SDL_LoadBMP("neil-googe-tiles.bmp");
    SDL_Texture * texture = SDL_CreateTextureFromSurface(sdlRenderer, image);

    SDL_FreeSurface(image);


    SDL_Event Event;

    bool Running = true;

    SDL_SetRenderDrawColor(sdlRenderer, 0, 0, 0, 255);

	while(Running) {
		while(SDL_PollEvent(&Event) != 0) {
			if(Event.type == SDL_QUIT) Running = false;
		}

        SDL_RenderCopy(sdlRenderer, texture, NULL, NULL);

        SDL_RenderPresent(sdlRenderer);

		SDL_Delay(1);
	}

	SDL_DestroyTexture(texture);
	SDL_DestroyRenderer(sdlRenderer);
	SDL_DestroyWindow(sdlWindow);

}