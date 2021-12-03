import { Tag } from "./Tag";

export interface IObject {
  name: string;
  notes: string;
  guid: string;
  tags: Tag[];
  path: string;
}