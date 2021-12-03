enum Direction {
  UNSPECIFIED,
  SOURCE_TO_DEST,
  BIDIRECTIONAL,
  DEST_TO_SOURCE,
}

namespace Direction {
  export function parse(directionString: string): Direction {
    switch(directionString.toLowerCase()){
      case 'source -> destination':
        return Direction.SOURCE_TO_DEST;

      case 'destination -> source':
        return Direction.DEST_TO_SOURCE;

      case 'bi-directional':
        return Direction.BIDIRECTIONAL;

      case 'unspecified':
        return Direction.UNSPECIFIED;

      default:
        throw new Error(`[Direction]: invalid destination`);
    }
  }
}

export default Direction;