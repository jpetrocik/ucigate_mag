#define BOARD_V2_3

#ifdef BOARD_V2_3 
  #define GATE 6
  #define LIGHT_RED 5 
  #define LIGHT_YELLOW_1 4
  #define LIGHT_YELLOW_2 3
  #define LIGHT_GREEN 2
  #define SPEAKER 9
  #define START 12
  #define TIMER 0
#endif


#ifdef BOARD_V2_2 
  #define GATE 2
  #define LIGHT_RED 3 
  #define LIGHT_YELLOW_1 4
  #define LIGHT_YELLOW_2 5
  #define LIGHT_GREEN 6
  #define SPEAKER 9
  #define START 7
  #define TIMER 0
#endif

/**
 * Changable for boards where UART is a SerialX
 */
#define SerialPort Serial
