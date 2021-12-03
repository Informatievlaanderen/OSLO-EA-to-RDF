import { Attribute } from "./Attribute";
import { Connector } from "./Connector";
import { IObject } from "./IObject";
import { Package } from "./Package";
import Type from "./Type";

export interface Element extends IObject {
  stereotype: string;
  type: Type;
  package: Package;
  attributes: Attribute[];
  connectors: Connector[];
}