enum Type {
  "CLASS",
  "DATATYPE",
  "ENUMERATION"
}

namespace Type {
  export function parse(typeString: string): Type {
    switch (typeString.toLowerCase()) {
      case 'class':
        return Type.CLASS;

      case 'datatype':
        return Type.DATATYPE;

      case 'enumeration':
        return Type.ENUMERATION;

      default:
        throw new Error(`[Type]: Invalid type.`);
    }
  }
}

export default Type;