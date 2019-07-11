/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlesnake;

import com.battlesnake.data.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class RequestController {

    @RequestMapping(value="/start", method=RequestMethod.POST, produces="application/json")
    public StartResponse start(@RequestBody StartRequest request) {
        return new StartResponse()
                .setName("radiant6, I love snacks")
                .setColor("#25da3d")
                .setHeadUrl("https://i.redd.it/6auki8gjsgw01.jpg")
                .setHeadType(HeadType.DEAD)
                .setTailType(TailType.PIXEL)
                .setTaunt("I can find food, I think!");
    }

    @RequestMapping(value="/move", method=RequestMethod.POST, produces = "application/json")
    public MoveResponse move(@RequestBody MoveRequest request) {
        MoveResponse moveResponse = new MoveResponse();
        
        Snake mySnake = findOurSnake(request); // kind of handy to have our snake at this level
        Snake otherSnake = null;
        for(Snake s : request.getSnakes()) {
            if (!s.getId().equals(mySnake.getId())) {
                otherSnake = s;
            }
        }

        Move wantedMove = findNextMove(mySnake, request, request.getFood()[0]);
        Move otherSnakeWantedMove = findNextMove(otherSnake, request, request.getFood()[0]);

        int[] snakeFood = nextMoveCoordinates(otherSnake.getCoords()[0], otherSnakeWantedMove);
        if (SnakeLength.isLonger(mySnake, otherSnake)) {
            System.out.println("Attack Mode!");
            wantedMove = findNextMove(mySnake, request, snakeFood);
        }

        System.out.println("Current: " + printXY(mySnake.getCoords()[0]));
        System.out.println("Next: " + printXY(nextMoveCoordinates(mySnake.getCoords()[0], wantedMove)));
        System.out.println("Will die: " + willDie(request, nextMoveCoordinates(mySnake.getCoords()[0], wantedMove)));
        return moveResponse.setMove(wantedMove).setTaunt("I'm hungry");
    }

    @RequestMapping(value="/end", method=RequestMethod.POST)
    public Object end() {
        // No response required∂
        Map<String, Object> responseObject = new HashMap<String, Object>();
        return responseObject;
    }

    /*
     *  Go through the snakes and find your team's snake
     *  
     *  @param  request The MoveRequest from the server
     *  @return         Your team's snake
     */
    private Snake findOurSnake(MoveRequest request) {
        String myUuid = request.getYou();
        List<Snake> snakes = request.getSnakes();
        return snakes.stream().filter(thisSnake -> thisSnake.getId().equals(myUuid)).findFirst().orElse(null);
    }

    private Move findNextMove(Snake snake, MoveRequest request, int[] food) {
        List<MoveChoice> foodMoveChoices = moveTowardsFood(request, snake.getCoords()[0], food);
        Move wantedMove = Move.DOWN;
        for(MoveChoice choice : foodMoveChoices) {
            if (!willDie(request, nextMoveCoordinates(snake.getCoords()[0], choice.move))) {
                wantedMove = choice.move;
                break;
            }
        }
        return wantedMove;
    }

    private class MoveChoice implements Comparable<MoveChoice> {
        public Move move;
        public int xdelta;
        public int ydelta;
        public int distance;
        public MoveChoice(Move move, int xdelta, int ydelta) {
            this.move = move;
            this.xdelta = xdelta;
            this.ydelta = ydelta;
            this.distance = xdelta * xdelta + ydelta * ydelta;
        }
        public int compareTo(final MoveChoice other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    /*
     *  Simple algorithm to find food
     *  
     *  @param  request The MoveRequest from the server
     *  @param  request An integer array with the X,Y coordinates of your snake's head
     *  @return         A Move that gets you closer to food
     */    
    public ArrayList<MoveChoice> moveTowardsFood(MoveRequest request, int[] mySnakeHead, int[] food) {
        int[] firstFoodLocation = food;
        int xdelta = firstFoodLocation[0] - mySnakeHead[0];
        int ydelta = firstFoodLocation[1] - mySnakeHead[1];

        ArrayList<MoveChoice> choices = new ArrayList<>();
        choices.add(new MoveChoice(Move.LEFT, xdelta + 1, ydelta));
        choices.add(new MoveChoice(Move.RIGHT, xdelta - 1, ydelta));
        choices.add(new MoveChoice(Move.UP, xdelta, ydelta + 1));
        choices.add(new MoveChoice(Move.DOWN, xdelta, ydelta - 1));


        Collections.sort(choices);

        return choices;
    }

    public int[] nextMoveCoordinates(int[] currentXY, Move move) {
        int[] newXY = new int[2];
        newXY[0] = currentXY[0];
        newXY[1] = currentXY[1];
        switch (move) {
            case UP:
                newXY[1] = newXY[1] - 1;
                break;
            case DOWN:
                newXY[1] = newXY[1] + 1;
                break;
            case LEFT:
                newXY[0] = newXY[0] - 1;
                break;
            case RIGHT:
                newXY[0] = newXY[0] + 1;
                break;
        }
        return newXY;
    }

    /**
     *
     * @param moveRequest
     * @param XY
     * @return true if XY matches any snake's coordinates
     */
    public boolean willDie(MoveRequest moveRequest, int[] XY) {
        if (XY[0] < 0 || XY[1] < 0) {
            return true;
        }
        if ((XY[0] > moveRequest.getWidth() - 1) ||
                XY[1] > moveRequest.getHeight() -1) {
            return true;
        }

        for (Snake s : moveRequest.getSnakes()) {
            for (int[] blocker : s.getCoords()) {
                if (blocker[0] == XY[0] && blocker[1] == XY[1]) {
                    System.out.println("Will collide with " + s.getName() + printXY(blocker));
                    return true;
                }
            }
        }
        return false;
    }

    public String printXY(int[] XY) {
        return "(" + Integer.toString(XY[0]) + ", " + Integer.toString(XY[1]) + ")";
    }
}
