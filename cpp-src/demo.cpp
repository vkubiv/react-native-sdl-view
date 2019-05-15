#include "SDL.h"

const Uint32 CHANGE_TEXTURE_EVENT = SDL_USEREVENT + 1;

extern C_LINKAGE SDLMAIN_DECLSPEC void SetTexturePath(const char * path) {

    char * pathCopy = new char[strlen(path) + 1];
    strcpy(pathCopy, path);

    SDL_Event event;
    SDL_zero(event);
    event.type = CHANGE_TEXTURE_EVENT;
    event.user.data1 = pathCopy;
    SDL_PushEvent(&event);
}

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
    SDL_Texture * texture = NULL;

    SDL_Event event;

    bool Running = true;

    SDL_SetRenderDrawColor(sdlRenderer, 0, 0, 0, 255);

	while(Running) {
		while(SDL_PollEvent(&event) != 0) {
			if(event.type == SDL_QUIT) {
                Running = false;
            }
			if (event.type == CHANGE_TEXTURE_EVENT) {
			    if (texture) {
                    SDL_DestroyTexture(texture);
			    }
			    char * path = static_cast<char *>(event.user.data1);
                SDL_Surface * image = SDL_LoadBMP((path));
                delete [] path;
                texture = SDL_CreateTextureFromSurface(sdlRenderer, image);
                SDL_FreeSurface(image);
			}
		}

		if (texture) {
            SDL_RenderCopy(sdlRenderer, texture, NULL, NULL);
        }

        SDL_RenderPresent(sdlRenderer);

		SDL_Delay(1);
	}

	if (texture) {
        SDL_DestroyTexture(texture);
    }
	SDL_DestroyRenderer(sdlRenderer);
	SDL_DestroyWindow(sdlWindow);

}